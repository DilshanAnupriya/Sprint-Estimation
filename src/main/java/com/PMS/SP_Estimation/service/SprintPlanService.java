package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.SprintPlanRequest;
import com.PMS.SP_Estimation.dto.request.VelocityPredictionRequest;
import com.PMS.SP_Estimation.dto.response.SprintPlanResponse;
import com.PMS.SP_Estimation.dto.response.StoryPointEstimateResponse;
import com.PMS.SP_Estimation.dto.response.VelocityPredictionResponse;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines velocity prediction + optional bulk story-point estimation into
 * a single sprint planning call.
 *
 * Workflow:
 *  1. Call VelocityMLService to predict next-sprint velocity.
 *  2. Load the sprint/project backlog.
 *  3. If estimateAll=true, run the SP ML model on every unestimated item.
 *  4. Build a feasibility summary (plannedSP vs recommendedMax) + warnings.
 */
@Service
@RequiredArgsConstructor
public class SprintPlanService {

    private final VelocityMLService     velService;
    private final StoryPointMLService   spService;
    private final BacklogItemRepository backlogRepo;

    public SprintPlanResponse plan(SprintPlanRequest req) {

        // 1 ── Velocity prediction ────────────────────────────────────
        VelocityPredictionRequest velReq = new VelocityPredictionRequest();
        velReq.setSprintId(req.getSprintId());
        velReq.setAvgVelocityLast3(req.getAvgVelocityLast3());
        velReq.setAvgVelocityLast5(req.getAvgVelocityLast5());
        velReq.setTeamSize(req.getTeamSize());
        velReq.setSprintDurationDays(req.getSprintDurationDays());
        velReq.setCompletionRate(req.getCompletionRate());
        velReq.setCarryoverTasks(req.getCarryoverTasks());
        velReq.setDeveloperAvailability(req.getDeveloperAvailability());
        velReq.setLeaveDays(req.getLeaveDays());
        velReq.setPlannedStoryPoints(req.getPlannedStoryPoints());
        velReq.setNumBugs(req.getNumBugs());
        velReq.setAvgExperienceYears(req.getAvgExperienceYears());
        velReq.setDomain(req.getDomain());

        VelocityPredictionResponse velPred = velService.predict(velReq);

        // 2 ── Load backlog scope ─────────────────────────────────────
        Long projectId = req.getProjectId();
        List<BacklogItem> items;

        if (req.getSprintId() != null) {
            items = backlogRepo.findBySprintId(req.getSprintId());
            if (projectId == null && !items.isEmpty() && items.get(0).getProject() != null) {
                projectId = items.get(0).getProject().getId();
            }
        } else if (projectId != null) {
            items = backlogRepo.findByProjectId(projectId);
        } else {
            items = List.of();
        }

        // 3 ── Optionally ML-estimate unestimated items ───────────────
        List<StoryPointEstimateResponse> estimates = new ArrayList<>();
        if (req.isEstimateAll()) {
            for (BacklogItem item : items) {
                if (item.getMlPointEstimate() == null) {
                    try {
                        estimates.add(spService.estimate(item.getId()));
                    } catch (Exception ignored) {
                        // ML service may be unavailable — skip item, don't fail whole plan
                    }
                }
            }
            // Reload so story_points reflect the ML estimates just persisted
            if (req.getSprintId() != null) {
                items = backlogRepo.findBySprintId(req.getSprintId());
            } else if (projectId != null) {
                items = backlogRepo.findByProjectId(projectId);
            }
        }

        // 4 ── Feasibility summary ────────────────────────────────────
        int totalSP = items.stream()
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        long unestimated = items.stream()
            .filter(i -> i.getStoryPoints() == null || i.getStoryPoints() == 0).count();

        int recommendedMax = velPred.getRecommendedCommitment() != null
            ? velPred.getRecommendedCommitment()
            : (int) Math.round(velPred.getPredictedVelocity() * 0.85);

        boolean feasible = totalSP <= recommendedMax;
        int surplus      = totalSP - recommendedMax;

        // 5 ── Warnings ────────────────────────────────────────────────
        List<String> warnings = new ArrayList<>();
        if (!feasible)
            warnings.add(String.format(
                "Planned %d SP exceeds recommended %d SP (85%% of predicted velocity). " +
                "Consider moving %d SP to the product backlog.", totalSP, recommendedMax, surplus));
        if (unestimated > 0)
            warnings.add(unestimated + " item(s) still have no story point estimate. " +
                "Run /backlog/{id}/estimate or set estimateAll=true.");
        if ("HIGH".equals(velPred.getRiskLevel()))
            warnings.add("High velocity risk detected (high stress score). " +
                "Reduce scope or address carryover / leave / bugs before sprint start.");
        long ambiguous = items.stream()
            .filter(i -> Boolean.TRUE.equals(i.getIsAmbiguous())).count();
        if (ambiguous > 0)
            warnings.add(ambiguous + " item(s) are flagged as ambiguous. " +
                "Refine user story text before committing to sprint.");

        return SprintPlanResponse.builder()
            .sprintId(req.getSprintId())
            .projectId(projectId)
            .velocityPrediction(velPred)
            .storyPointEstimates(estimates)
            .estimatedItemCount(estimates.size())
            .totalPlannedSP(totalSP)
            .unestimatedItems((int) unestimated)
            .recommendedMaxSP(recommendedMax)
            .isFeasible(feasible)
            .surplusSP(surplus)
            .warnings(warnings)
            .build();
    }
}
