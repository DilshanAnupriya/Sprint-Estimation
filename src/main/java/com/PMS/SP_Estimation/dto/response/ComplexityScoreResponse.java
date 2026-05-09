package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplexityScoreResponse {
    private Long    backlogItemId;
    private String  title;

    // Raw components (from dataset fields)
    private int     numComponents;
    private int     externalApis;
    private boolean hasIntegration;
    private boolean hasSecurity;
    private boolean hasUiComplexity;
    private int     wordCount;

    // Computed
    private double  complexityScore;        // 0.0 – 3.0+ scale
    private String  complexityBand;         // LOW | MEDIUM | HIGH | VERY_HIGH
    private boolean isAmbiguous;            // word_count ≤ 5 AND high SP
    private String  ambiguityReason;
    private String  recommendation;
}
