package com.PMS.SP_Estimation.dto.request;

import lombok.Data;

@Data
public class SprintMetricsRequest {
    private Double  developerAvailability;
    private Integer leaveDaysTotal;
    private Integer numBugsThisSprint;
    private Integer carryoverTasks;
    private Double  completionRate;
}
