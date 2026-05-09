package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.response.SprintHealthResponse;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SprintHealthService {

    private final SprintRepository      sprintRepo;
    private final BacklogItemRepository backlogRepo;
    private final VelocityMLService     velService;

    // Domain velocity averages — derived from SV dataset analysis
    private static final Map<String, Double> DOMAIN_AVG_VELOCITY = Map.of(
        "Finance",    26.31,
        "Logistics",  26.08,
        "E-commerce", 24.16,
        "Education",  23.84,
        "Healthcare", 22.17
    );

    /**
     * Full sprint health computation.
     * Based on SV dataset field analysis and correlations.
     *
     * Health Score formula (0.0–1.0):
     *   completion_rate        × 0.35   (strongest non-velocity signal: corr 0.191)
     *   (1 - carryover/23)     × 0.25   (carryover max=23 in dataset)
     *   developer_availability × 0.25   (corr 0.232 with next velocity)
     *   (1 - num_bugs/10)      × 0.15   (bugs max=10 in SV dataset)
     */
    public SprintHealthResponse computeHealth(Long sprintId) {
        Sprint sprint = sprintRepo.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        // Pull stored sprint metrics (set by SprintService.completeSprint or updateMetrics)
        double completionRate   = sprint.getCompletionRate()          != null ? sprint.getCompletionRate()          : 0.81;
        int    carryover        = sprint.getCarryoverTasks()          != null ? sprint.getCarryoverTasks()          : 0;
        double availability     = sprint.getDeveloperAvailability()   != null ? sprint.getDeveloperAvailability()   : 1.0;
        int    bugs             = sprint.getNumBugsThisSprint()       != null ? sprint.getNumBugsThisSprint()       : 0;
        int    leaveDays        = sprint.getLeaveDaysTotal()          != null ? sprint.getLeaveDaysTotal()          : 0;
        double avgVel3          = sprint.getAvgVelocityLast3()        != null ? sprint.getAvgVelocityLast3()        : 0.0;
        double avgVel5          = sprint.getAvgVelocityLast5()        != null ? sprint.getAvgVelocityLast5()        : 0.0;
        int    teamSize         = (sprint.getProject() != null && sprint.getProject().getTeamSize() != null)
                                    ? sprint.getProject().getTeamSize() : 5;
        int    durationDays     = sprint.getDurationDays()            != null ? sprint.getDurationDays()            : 14;
        int    plannedSP        = backlogRepo.findBySprintId(sprintId).stream()
                                    .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();

        // Health score (0–1)
        double health = completionRate             * 0.35
                      + (1.0 - Math.min(carryover / 23.0, 1.0)) * 0.25
                      + availability              * 0.25
                      + (1.0 - Math.min(bugs / 10.0, 1.0))      * 0.15;
        health = Math.min(Math.max(health, 0.0), 1.0);

        String healthBand = health >= 0.85 ? "EXCELLENT"
                          : health >= 0.70 ? "GOOD"
                          : health >= 0.55 ? "FAIR"
                          : health >= 0.40 ? "POOR"
                          : "CRITICAL";

        // Velocity trend: avg_last3 - avg_last5
        // From dataset: 52% declining, 46.7% improving
        double velocityTrend = avgVel3 - avgVel5;
        String trendDir      = velocityTrend > 1.0  ? "IMPROVING"
                             : velocityTrend < -1.0 ? "DECLINING"
                             : "STABLE";

        // Velocity per developer — team efficiency metric
        double velPerDev = teamSize > 0 ? avgVel3 / teamSize : 0.0;

        // Effective capacity = team_size × availability × (duration/14)
        // From dataset: mean=5.935 × 0.773 × 1.047 ≈ 4.8 effective devs
        double effectiveCap = teamSize * availability * (durationDays / 14.0);

        // Workload ratio — 28.6% of dataset sprints are overcommitted (>1.1)
        double workloadRatio = effectiveCap > 0 ? plannedSP / (effectiveCap * 8) : 1.0;
        boolean overcommitted = workloadRatio > 1.1;

        // Burnout risk — 7.5% of dataset sprints
        // Condition: leave_days ≥ 3 AND carryover_tasks ≥ 8 AND num_bugs ≥ 6
        boolean burnoutRisk = leaveDays >= 3 && carryover >= 8 && bugs >= 6;

        int recommended = sprint.getPredictedVelocity() != null
            ? (int)(sprint.getPredictedVelocity() * 0.85) : (int)(avgVel3 * 0.85);

        // Domain benchmark comparison
        String domain = sprint.getProject() != null ? sprint.getProject().getDomain() : null;
        double domainAvg = DOMAIN_AVG_VELOCITY.getOrDefault(
            capitalise(domain), 24.45);  // 24.45 = dataset overall mean

        // Build warnings and recommendations
        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (burnoutRisk) {
            warnings.add("BURNOUT RISK: High leave days, carryover, and bugs detected.");
            recommendations.add("Reduce sprint scope. Allow recovery time for the team.");
        }
        if (overcommitted) {
            warnings.add(String.format("OVERCOMMITTED: workload ratio %.2f exceeds 1.1 threshold "
                + "(28.6%% of sprints in our dataset are overcommitted).", workloadRatio));
            recommendations.add("Reduce planned SP to " + recommended + " (85% of predicted velocity).");
        }
        if ("DECLINING".equals(trendDir)) {
            warnings.add(String.format("DECLINING VELOCITY: last3=%.1f vs last5=%.1f (trend=%.1f SP)",
                avgVel3, avgVel5, velocityTrend));
            recommendations.add("Investigate root cause — check carryover, team changes, or technical debt.");
        }
        if (carryover >= 8) {
            warnings.add("HIGH CARRYOVER: " + carryover + " tasks carried over "
                + "(dataset mean=6.4, max=23). Indicates consistent overcommitment.");
            recommendations.add("Use carryover tasks to estimate true capacity before planning next sprint.");
        }
        if (completionRate < 0.70) {
            warnings.add(String.format("LOW COMPLETION RATE: %.0f%% (dataset mean=81%%)",
                completionRate * 100));
            recommendations.add("Reassess story point estimates and team capacity.");
        }
        if (avgVel3 < domainAvg * 0.80) {
            warnings.add(String.format("BELOW DOMAIN BENCHMARK: avg velocity %.1f vs "
                + "%s domain average %.1f SP", avgVel3, domain, domainAvg));
        }
        if (warnings.isEmpty()) {
            recommendations.add("Team health looks good. Sprint commitment is realistic.");
        }

        // Persist health score back to sprint
        sprint.setHealthScore(health);
        sprint.setIsOvercommitted(overcommitted);
        sprint.setBurnoutRisk(burnoutRisk);
        sprint.setVelocityTrend(velocityTrend);
        sprint.setEffectiveCapacity(effectiveCap);
        sprint.setVelocityPerDev(velPerDev);
        sprintRepo.save(sprint);

        return SprintHealthResponse.builder()
            .sprintId(sprintId)
            .sprintName(sprint.getName())
            .completionRate(completionRate)
            .carryoverTasks(carryover)
            .developerAvailability(availability)
            .numBugs(bugs)
            .leaveDays(leaveDays)
            .avgVelocityLast3(avgVel3)
            .avgVelocityLast5(avgVel5)
            .healthScore(Math.round(health * 1000.0) / 1000.0)
            .healthBand(healthBand)
            .velocityTrend(Math.round(velocityTrend * 10.0) / 10.0)
            .trendDirection(trendDir)
            .velocityPerDev(Math.round(velPerDev * 100.0) / 100.0)
            .effectiveCapacity(Math.round(effectiveCap * 100.0) / 100.0)
            .isOvercommitted(overcommitted)
            .burnoutRisk(burnoutRisk)
            .recommendedCommitment(recommended)
            .warnings(warnings)
            .recommendations(recommendations)
            .build();
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
