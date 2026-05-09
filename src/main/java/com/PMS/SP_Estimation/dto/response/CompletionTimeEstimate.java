package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompletionTimeEstimate {
    private int     storyPoints;
    private double  estimatedHours;
    private double  estimatedDays;          // hours / 8
    private String  confidenceBand;         // based on risk_level from ML
}
