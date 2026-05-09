package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.EstimationOverrideRequest;
import com.PMS.SP_Estimation.dto.response.StoryPointEstimateResponse;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.entity.Project;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.entity.TeamMember;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.SprintRepository;
import com.PMS.SP_Estimation.repo.TeamMemberRepository;
import com.PMS.SP_Estimation.service.ml.MLClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoryPointMLService {

    private final BacklogItemRepository backlogRepo;
    private final SprintRepository      sprintRepo;
    private final TeamMemberRepository  teamRepo;
    private final MLClient              mlClient;

    // Hours-per-SP lookup (dataset-derived, matches ComplexityService)
    private static final Map<Integer, Double> HOURS_PER_SP = Map.of(
        1,  5.6,
        2,  9.5,
        3,  13.3,
        5,  22.7,
        8,  36.2,
        13, 58.5
    );

    public StoryPointEstimateResponse estimate(Long itemId) {
        return estimate(itemId, null);
    }

    /**
     * Predict story points for a backlog item.
     *
     * Precedence for every ML input is:
     *   1. override field on EstimationOverrideRequest (if present)
     *   2. value on the BacklogItem entity
     *   3. project default
     *   4. hardcoded sensible fallback
     *
     * This mirrors the notebook's interactive prompt flow — the frontend can
     * collect inputs from the user and pass them as overrides so brand-new
     * projects (no team members, no sprint history) still get accurate
     * predictions.
     */
    public StoryPointEstimateResponse estimate(Long itemId, EstimationOverrideRequest ov) {
        BacklogItem item = backlogRepo.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Backlog item not found"));

        Project project = item.getProject();
        if (project == null)
            throw new ResourceNotFoundException("Backlog item is not attached to a project");

        Long projectId = project.getId();

        // ── Project-derived context (used as fallback) ────────────────────
        List<Sprint> completed = sprintRepo.findByProjectId(projectId).stream()
            .filter(s -> s.getStatus() == Sprint.Status.COMPLETED).toList();

        int derivedTeamVelocity = (int) completed.stream()
            .map(Sprint::getActualVelocity)
            .filter(v -> v != null && v > 0)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(project.getDefaultTeamVelocity() != null ? project.getDefaultTeamVelocity() : 20);

        double derivedRecentCompletion = completed.stream()
            .map(Sprint::getCompletionRate)
            .filter(r -> r != null && r > 0)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(project.getDefaultCompletionRate() != null ? project.getDefaultCompletionRate() : 0.85);

        int derivedSimilarTaskCount = (int) backlogRepo.findByProjectId(projectId).stream()
            .filter(b -> !b.getId().equals(itemId))
            .filter(b -> b.getTaskType() == item.getTaskType())
            .filter(b -> b.getStatus() == BacklogItem.Status.DONE)
            .count();

        int derivedTaskAgeDays = item.getCreatedAt() != null
            ? (int) Duration.between(item.getCreatedAt(), LocalDateTime.now()).toDays() : 0;

        int derivedTeamSize = project.getTeamSize() != null && project.getTeamSize() > 0
            ? project.getTeamSize()
            : Math.max((int) teamRepo.countByProjectId(projectId), 1);

        int derivedSprintDuration = project.getDefaultSprintDurationDays() != null
            ? project.getDefaultSprintDurationDays() : 14;

        TeamMember.ExperienceLevel derivedLevel = inferExperienceLevel(
            teamRepo.findByProjectId(projectId), project.getDefaultDevExperienceLevel());

        // Tech stack precedence: item > sprint > project default > hardcoded fallback
        String derivedTechStack;
        if (item.getTechStack() != null && !item.getTechStack().isBlank()) {
            derivedTechStack = item.getTechStack();
        } else if (item.getSprint() != null && item.getSprint().getTechStack() != null
                   && !item.getSprint().getTechStack().isBlank()) {
            derivedTechStack = item.getSprint().getTechStack();
        } else {
            derivedTechStack = project.getDefaultTechStack();
        }
        if (derivedTechStack == null || derivedTechStack.isBlank()) derivedTechStack = "springboot";

        String derivedDomain = project.getDomain();   // null → caller MUST provide via override or project setup

        // ── Apply overrides on top of derived values ──────────────────────
        String  domain                 = pick(ov != null ? ov.getDomain()              : null, derivedDomain);
        String  techStack              = pick(ov != null ? ov.getTechStack()           : null, derivedTechStack);
        TeamMember.ExperienceLevel lvl = pick(ov != null ? ov.getDevExperienceLevel()  : null, derivedLevel);
        int     teamSize               = pick(ov != null ? ov.getTeamSize()            : null, derivedTeamSize);
        int     sprintDuration         = pick(ov != null ? ov.getSprintDurationDays()  : null, derivedSprintDuration);
        int     teamVelocityAvg        = pick(ov != null ? ov.getTeamVelocityAvg()     : null, derivedTeamVelocity);
        double  recentCompletion       = pick(ov != null ? ov.getRecentCompletionRate(): null, derivedRecentCompletion);
        int     similarTaskCount       = pick(ov != null ? ov.getSimilarTaskCount()    : null, derivedSimilarTaskCount);
        int     taskAgeDays            = pick(ov != null ? ov.getTaskAgeDays()         : null, derivedTaskAgeDays);

        Integer numComponents          = pickBoxed(ov != null ? ov.getNumComponents()  : null, item.getNumComponents(), 1);
        Integer externalApis           = pickBoxed(ov != null ? ov.getExternalApis()   : null, item.getExternalApis(),  0);

        // Concatenate title + userStory so TF-IDF gets richer signal even when the
        // stored userStory field is just a short description (e.g. "login feature with google auth").
        // Override wins outright; otherwise merge title + stored userStory.
        String itemRichText;
        if (item.getUserStory() != null && !item.getUserStory().isBlank()) {
            String t = item.getTitle() != null ? item.getTitle().trim() : "";
            String s = item.getUserStory().trim();
            itemRichText = t.isBlank() ? s : t + " " + s;
        } else {
            itemRichText = item.getTitle();
        }
        String userStory = (ov != null && ov.getUserStory() != null && !ov.getUserStory().isBlank())
                           ? ov.getUserStory() : itemRichText;

        // Boolean flags: override > item field > keyword-inferred from story text.
        // Auto-inference closes the gap when the frontend doesn't collect these inputs explicitly.
        Boolean hasIntegration  = pickBoolInfer(ov != null ? ov.getHasIntegration() : null,
                                                item.getHasIntegration(),
                                                inferFlag(userStory, "integrat", "webhook", "connect",
                                                          "third-party", "third party", "google", "facebook",
                                                          "twitter", "stripe", "paypal", "oauth", "sso", "ldap",
                                                          "saml", "external", "microservice"));
        Boolean hasSecurity     = pickBoolInfer(ov != null ? ov.getHasSecurity() : null,
                                                item.getHasSecurity(),
                                                inferFlag(userStory, "auth", "login", "logout", "signup",
                                                          "register", "password", "secur", "permission",
                                                          "role", "access control", "jwt", "token",
                                                          "encrypt", "2fa", "mfa", "otp", "captcha"));
        Boolean hasUiComplexity = pickBoolInfer(ov != null ? ov.getHasUiComplexity() : null,
                                                item.getHasUiComplexity(),
                                                inferFlag(userStory, "dashboard", "chart", "graph", "table",
                                                          "filter", "sort", "search", "pagination", "modal",
                                                          "wizard", "carousel", "drag", "calendar",
                                                          "rich text", "editor", "upload", "preview"));

        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException(
                "Domain is required for SP estimation. Set project.domain or pass it as an override.");
        }

        // ── ML inference ──────────────────────────────────────────────────
        // Task type: override > item entity > "feature"
        String taskType = (ov != null && ov.getTaskType() != null && !ov.getTaskType().isBlank())
            ? ov.getTaskType().toLowerCase()
            : (item.getTaskType() != null ? item.getTaskType().name().toLowerCase() : "feature");

        MLClient.StoryPointPredictRequest mlReq = MLClient.StoryPointPredictRequest.builder()
            .userStory(userStory != null ? userStory : "")
            .taskType(taskType)
            .domain(domain.toLowerCase())
            .techStack(techStack.toLowerCase())
            .devExperienceLevel(lvl.name().toLowerCase())
            .numComponents(numComponents)
            .externalApis(externalApis)
            .hasIntegration(Boolean.TRUE.equals(hasIntegration)  ? 1 : 0)
            .hasSecurity(Boolean.TRUE.equals(hasSecurity)        ? 1 : 0)
            .hasUiComplexity(Boolean.TRUE.equals(hasUiComplexity) ? 1 : 0)
            .teamSize(teamSize)
            .sprintDurationDays(sprintDuration)
            .similarTaskCount(similarTaskCount)
            .teamVelocityAvg(teamVelocityAvg > 0 ? teamVelocityAvg : 20)
            .recentCompletionRate(recentCompletion)
            .taskAgeDays(taskAgeDays)
            .build();

        MLClient.StoryPointPrediction p = mlClient.predictStoryPoint(mlReq);

        // ── Persist ML outputs ────────────────────────────────────────────
        item.setMlPointEstimate(p.getPointEstimate());
        item.setMlLowerBound(p.getLowerBound());
        item.setMlUpperBound(p.getUpperBound());
        item.setEstimationRisk(p.getRiskLevel());
        if (item.getStoryPoints() == null || item.getStoryPoints() == 0) {
            item.setStoryPoints(p.getPointEstimate());
        }

        // ── Auto-compute complexity + ambiguity + estimated hours ─────────
        int     wordCount = userStory != null ? userStory.trim().split("\\s+").length : 0;
        boolean hasInteg  = Boolean.TRUE.equals(hasIntegration);
        boolean hasSec    = Boolean.TRUE.equals(hasSecurity);
        boolean hasUiCx   = Boolean.TRUE.equals(hasUiComplexity);

        double score = numComponents * 0.25
                     + externalApis  * 0.20
                     + (hasInteg ? 1 : 0) * 0.15
                     + (hasSec   ? 1 : 0) * 0.15
                     + (hasUiCx  ? 1 : 0) * 0.10
                     + (wordCount / 19.0)  * 0.15;
        item.setComplexityScore(Math.round(score * 1000.0) / 1000.0);

        boolean ambiguous = wordCount <= 5 && (numComponents >= 3 || externalApis >= 1);
        item.setIsAmbiguous(ambiguous);
        item.setAmbiguityReason(ambiguous
            ? "User story is too brief (" + wordCount + " words) for a complex task."
            : null);

        double baseHours = HOURS_PER_SP.entrySet().stream()
            .min(Comparator.comparingInt(e -> Math.abs(e.getKey() - p.getPointEstimate())))
            .map(Map.Entry::getValue)
            .orElse(p.getPointEstimate() * 4.5);
        if ("HIGH".equals(p.getRiskLevel()))   baseHours *= 1.20;
        if ("MEDIUM".equals(p.getRiskLevel())) baseHours *= 1.10;
        item.setEstimatedHours(Math.round(baseHours * 10.0) / 10.0);

        item.setUpdatedAt(LocalDateTime.now());
        backlogRepo.save(item);

        return StoryPointEstimateResponse.builder()
            .backlogItemId(itemId)
            .pointEstimate(p.getPointEstimate())
            .lowerBound(p.getLowerBound())
            .upperBound(p.getUpperBound())
            .riskLevel(p.getRiskLevel())
            .rawPoint(p.getRawPoint())
            .rawP20(p.getRawP20())
            .rawP80(p.getRawP80())
            .usedFineTuned(p.getUsedFineTuned())
            .domainUsed(p.getDomainUsed())
            // Inputs actually sent to the ML model — visible to frontend for transparency.
            // ALL 16 fields are exposed so users can verify nothing was silently defaulted.
            .usedDomain(domain.toLowerCase())
            .usedTechStack(techStack.toLowerCase())
            .usedTaskType(taskType)
            .usedDevExperienceLevel(lvl.name().toLowerCase())
            .usedTeamSize(teamSize)
            .usedSprintDurationDays(sprintDuration)
            .usedTeamVelocityAvg(teamVelocityAvg)
            .usedRecentCompletionRate(recentCompletion)
            .usedSimilarTaskCount(similarTaskCount)
            .usedTaskAgeDays(taskAgeDays)
            .usedHasIntegration(hasIntegration)
            .usedHasSecurity(hasSecurity)
            .usedHasUiComplexity(hasUiComplexity)
            .usedNumComponents(numComponents)
            .usedExternalApis(externalApis)
            .usedUserStory(userStory)
            .build();
    }

    private static TeamMember.ExperienceLevel inferExperienceLevel(
            List<TeamMember> members, TeamMember.ExperienceLevel projectDefault) {
        if (members.stream().noneMatch(m -> m.getExperienceYears() != null)) {
            return projectDefault != null ? projectDefault : TeamMember.ExperienceLevel.MID;
        }
        double avg = members.stream()
            .filter(m -> m.getExperienceYears() != null)
            .mapToDouble(TeamMember::getExperienceYears)
            .average()
            .orElse(3.0);
        if (avg >= 6.0) return TeamMember.ExperienceLevel.SENIOR;
        if (avg >= 3.0) return TeamMember.ExperienceLevel.MID;
        return TeamMember.ExperienceLevel.JUNIOR;
    }

    // ── pick helpers: override > fallback ─────────────────────────────────
    private static <T> T   pick(T override, T fallback)         { return override != null ? override : fallback; }
    private static int     pick(Integer override, int fallback) { return override != null ? override : fallback; }
    private static double  pick(Double override, double fallback) { return override != null ? override : fallback; }
    private static Integer pickBoxed(Integer ov, Integer item, int last) {
        if (ov != null)   return ov;
        if (item != null) return item;
        return last;
    }
    private static Boolean pickBool(Boolean ov, Boolean item) {
        if (ov != null)   return ov;
        return item;
    }
    // Like pickBool but uses keyword-inferred value when both override and item field are unset
    private static Boolean pickBoolInfer(Boolean ov, Boolean item, boolean inferred) {
        if (ov   != null) return ov;
        if (item != null) return item;
        return inferred;
    }

    /** Returns true if any keyword appears (substring) in the lower-cased text. */
    private static boolean inferFlag(String text, String... keywords) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }
}
