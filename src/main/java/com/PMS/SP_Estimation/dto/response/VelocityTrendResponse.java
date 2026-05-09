package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class VelocityTrendResponse {
    private Long    projectId;
    private List<VelocityPoint> history;    // one per completed sprint
    private double  avgVelocityLast3;
    private double  avgVelocityLast5;
    private double  trend;                  // last3 – last5
    private String  trendDirection;
    private double  velocityPerDev;
    private String  domainBenchmark;        // vs dataset domain average
    private double  domainAvgVelocity;

    @Data
    @Builder
    public static class VelocityPoint {
        private Long      sprintId;
        private String    sprintName;
        private int       actualVelocity;
        private int       predictedVelocity;
        private double    completionRate;
        private LocalDate endDate;
    }
}
