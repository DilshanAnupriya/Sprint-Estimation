package com.PMS.SP_Estimation.dto.response;

import com.PMS.SP_Estimation.entity.Sprint;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SprintResponse {
    private Long          id;
    private Long          projectId;
    private String        projectName;     // populated from Project relationship
    private Integer       backlogItemCount; // # of items assigned to this sprint
    private Integer       totalPlannedSP;   // sum of storyPoints for assigned items
    private String        name;
    private Sprint.Status status;
    private LocalDate     startDate;
    private LocalDate     endDate;
    private Integer       durationDays;
    private String        techStack;

    private Integer predictedVelocity;
    private Integer actualVelocity;

    private Double  healthScore;
    private Double  velocityTrend;
    private Double  effectiveCapacity;
    private Boolean isOvercommitted;
    private Boolean burnoutRisk;
    private Double  velocityPerDev;
    private Double  completionRate;
    private Integer carryoverTasks;
    private Integer numBugsThisSprint;
    private Integer leaveDaysTotal;
    private Double  developerAvailability;
    private Double  avgVelocityLast3;
    private Double  avgVelocityLast5;
}
