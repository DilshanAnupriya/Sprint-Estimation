package com.PMS.SP_Estimation.dto.request;

import lombok.Data;

@Data
public class VelocityPredictionRequest {
    /** Sprint to score; service uses its team / project / metrics if other fields are blank. */
    private Long    sprintId;

    // Optional overrides (any null falls back to sprint/project state)
    private Double  avgVelocityLast3;
    private Double  avgVelocityLast5;
    private Integer teamSize;
    private Integer sprintDurationDays;
    private Double  completionRate;
    private Integer carryoverTasks;
    private Double  developerAvailability;
    private Integer leaveDays;
    private Integer plannedStoryPoints;
    private Integer numBugs;
    private Double  avgExperienceYears;
    private String  domain;
}
