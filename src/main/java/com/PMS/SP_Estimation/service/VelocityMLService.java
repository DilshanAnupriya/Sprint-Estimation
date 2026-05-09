package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.VelocityPredictionRequest;
import com.PMS.SP_Estimation.dto.response.VelocityPredictionResponse;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.entity.TeamMember;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.SprintRepository;
import com.PMS.SP_Estimation.repo.TeamMemberRepository;
import com.PMS.SP_Estimation.service.ml.MLClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VelocityMLService {

    private final SprintRepository      sprintRepo;
    private final TeamMemberRepository  teamRepo;
    private final BacklogItemRepository backlogRepo;
    private final VelocityAnalyticsService velocityAnalytics;
    private final MLClient mlClient;

    /**
     * Predict next-sprint velocity by combining stored sprint metrics with
     * the FastAPI ML service. Falls back to avg_velocity_last3 if a sprint
     * has no recorded metrics yet.
     */
    public VelocityPredictionResponse predict(VelocityPredictionRequest req) {
        Sprint sprint = req.getSprintId() == null ? null
            : sprintRepo.findById(req.getSprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        Long projectId = sprint != null && sprint.getProject() != null
            ? sprint.getProject().getId() : null;
        Map<String, Double> rolling = projectId != null
            ? velocityAnalytics.computeRollingVelocities(projectId)
            : Map.of("avg_velocity_last3", 0.0, "avg_velocity_last5", 0.0);

        double avg3 = orElse(req.getAvgVelocityLast3(), rolling.get("avg_velocity_last3"));
        double avg5 = orElse(req.getAvgVelocityLast5(), rolling.get("avg_velocity_last5"));

        int teamSize = orElse(req.getTeamSize(),
            sprint != null && sprint.getProject() != null && sprint.getProject().getTeamSize() != null
                ? sprint.getProject().getTeamSize()
                : projectId != null ? (int) teamRepo.countByProjectId(projectId) : 5);
        if (teamSize <= 0) teamSize = 5;

        int durationDays = orElse(req.getSprintDurationDays(),
            sprint != null && sprint.getDurationDays() != null ? sprint.getDurationDays() : 14);

        double completionRate = orElse(req.getCompletionRate(),
            sprint != null && sprint.getCompletionRate() != null ? sprint.getCompletionRate() : 0.81);
        int carryover = orElse(req.getCarryoverTasks(),
            sprint != null && sprint.getCarryoverTasks() != null ? sprint.getCarryoverTasks() : 0);
        double availability = orElse(req.getDeveloperAvailability(),
            sprint != null && sprint.getDeveloperAvailability() != null ? sprint.getDeveloperAvailability() : 1.0);
        int leaveDays = orElse(req.getLeaveDays(),
            sprint != null && sprint.getLeaveDaysTotal() != null ? sprint.getLeaveDaysTotal() : 0);
        int numBugs = orElse(req.getNumBugs(),
            sprint != null && sprint.getNumBugsThisSprint() != null ? sprint.getNumBugsThisSprint() : 0);

        int plannedSP = orElse(req.getPlannedStoryPoints(), sprint != null
            ? backlogRepo.findBySprintId(sprint.getId()).stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum()
            : 0);

        double avgExperience = orElse(req.getAvgExperienceYears(),
            projectId != null ? avgExperienceYears(teamRepo.findByProjectId(projectId)) : 4.0);

        String domain = req.getDomain() != null ? req.getDomain()
            : (sprint != null && sprint.getProject() != null) ? sprint.getProject().getDomain()
            : null;

        MLClient.VelocityPredictRequest mlReq = MLClient.VelocityPredictRequest.builder()
            .avgVelocityLast3(avg3)
            .avgVelocityLast5(avg5)
            .teamSize(teamSize)
            .sprintDurationDays(durationDays)
            .completionRate(completionRate)
            .carryoverTasks(carryover)
            .developerAvailability(availability)
            .leaveDays(leaveDays)
            .plannedStoryPoints(plannedSP)
            .numBugs(numBugs)
            .avgExperienceYears(avgExperience)
            .domain(domain)
            .build();

        MLClient.VelocityPrediction p = mlClient.predictVelocity(mlReq);

        // Persist on the sprint for downstream analytics
        if (sprint != null) {
            sprint.setPredictedVelocity(p.getPredictedVelocity());
            sprint.setAvgVelocityLast3(avg3);
            sprint.setAvgVelocityLast5(avg5);
            sprintRepo.save(sprint);
        }

        // # of completed sprints — useful for the frontend to know whether
        // rolling velocities are based on real history or zero
        int completedSprintCount = projectId == null ? 0
            : (int) sprintRepo.findByProjectId(projectId).stream()
                .filter(s -> s.getStatus() == Sprint.Status.COMPLETED && s.getActualVelocity() != null && s.getActualVelocity() > 0)
                .count();

        return VelocityPredictionResponse.builder()
            .sprintId(sprint != null ? sprint.getId() : null)
            .predictedVelocity(p.getPredictedVelocity())
            .rawPrediction(p.getRawPrediction())
            .basePrediction(p.getBasePrediction())
            .usedFineTuned(p.getUsedFineTuned())
            .domainUsed(p.getDomainUsed())
            .riskLevel(p.getRiskLevel())
            .stressScore(p.getStressScore())
            .velocityDelta(p.getVelocityDelta())
            .effectiveCapacity(p.getEffectiveCapacity())
            .velocityPerDev(p.getVelocityPerDev())
            .recommendedCommitment((int) Math.round(p.getPredictedVelocity() * 0.85))
            // Diagnostics — what the model actually saw
            .usedAvgVelocityLast3(avg3)
            .usedAvgVelocityLast5(avg5)
            .usedTeamSize(teamSize)
            .usedSprintDurationDays(durationDays)
            .usedCompletionRate(completionRate)
            .usedCarryoverTasks(carryover)
            .usedDeveloperAvailability(availability)
            .usedLeaveDays(leaveDays)
            .usedPlannedStoryPoints(plannedSP)
            .usedNumBugs(numBugs)
            .usedAvgExperienceYears(avgExperience)
            .usedDomain(domain)
            .usedCompletedSprintCount(completedSprintCount)
            .build();
    }

    /**
     * Convenience for callers that already have rolling velocities — keeps
     * the older signature alive during refactors. Not used by controllers.
     */
    public double quickPredict(double avgVelocityLast3, double avgVelocityLast5) {
        if (avgVelocityLast3 <= 0 && avgVelocityLast5 <= 0) return 24.45;
        if (avgVelocityLast3 <= 0) return avgVelocityLast5;
        return avgVelocityLast3;
    }

    private static double avgExperienceYears(List<TeamMember> members) {
        return members.stream()
            .filter(m -> m.getExperienceYears() != null)
            .mapToDouble(TeamMember::getExperienceYears)
            .average()
            .orElse(4.0);
    }

    private static double orElse(Double a, Double b) { return a != null ? a : (b != null ? b : 0.0); }
    private static int    orElse(Integer a, Integer b) { return a != null ? a : (b != null ? b : 0); }
    private static double orElse(Double a, double b)  { return a != null ? a : b; }
    private static int    orElse(Integer a, int b)    { return a != null ? a : b; }
}
