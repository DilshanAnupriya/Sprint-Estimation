package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * One point in the sprint velocity history chart.
 * Used by GET /api/v1/dashboard/projects/{id}/velocity-history
 */
@Data
@Builder
public class VelocityHistoryPoint {
    private Long      sprintId;
    private String    sprintName;
    private Integer   predictedVelocity;
    private Integer   actualVelocity;
    private Double    completionRate;
    private Double    healthScore;
    private LocalDate startDate;
    private LocalDate endDate;
}
