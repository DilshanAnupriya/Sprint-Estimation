package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SprintHealthResponse {
    private Long    sprintId;
    private String  sprintName;

    // Raw SV inputs
    private double  completionRate;
    private int     carryoverTasks;
    private double  developerAvailability;
    private int     numBugs;
    private int     leaveDays;
    private double  avgVelocityLast3;
    private double  avgVelocityLast5;

    // Computed
    private double  healthScore;             // 0.0 – 1.0
    private String  healthBand;              // CRITICAL | POOR | FAIR | GOOD | EXCELLENT
    private double  velocityTrend;           // last3 – last5
    private String  trendDirection;          // IMPROVING | STABLE | DECLINING
    private double  velocityPerDev;          // avg_velocity_last3 / team_size
    private double  effectiveCapacity;       // team_size × avail × (duration/14)
    private boolean isOvercommitted;         // workload_ratio > 1.1
    private boolean burnoutRisk;             // leave≥3 AND carryover≥8 AND bugs≥6
    private int     recommendedCommitment;   // predicted_velocity × 0.85
    private List<String> warnings;
    private List<String> recommendations;
}
