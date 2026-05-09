package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VelocityPredictionResponse {
    private Long    sprintId;
    private Integer predictedVelocity;
    private Double  rawPrediction;
    private Integer basePrediction;
    private Boolean usedFineTuned;
    private String  domainUsed;
    private String  riskLevel;
    private Double  stressScore;
    private Double  velocityDelta;
    private Double  effectiveCapacity;
    private Double  velocityPerDev;
    private Integer recommendedCommitment;

    // ── Inputs actually sent to the ML model ──────────────────────────────────
    // Lets the frontend show "what the AI saw" so users can debug bad
    // predictions (e.g. wrong domain, missing leave days, default 1.0 availability).
    private Double  usedAvgVelocityLast3;
    private Double  usedAvgVelocityLast5;
    private Integer usedTeamSize;
    private Integer usedSprintDurationDays;
    private Double  usedCompletionRate;
    private Integer usedCarryoverTasks;
    private Double  usedDeveloperAvailability;
    private Integer usedLeaveDays;
    private Integer usedPlannedStoryPoints;
    private Integer usedNumBugs;
    private Double  usedAvgExperienceYears;
    private String  usedDomain;
    private Integer usedCompletedSprintCount;
}
