package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class BacklogAnalysisResponse {
    private Long    projectId;
    private int     totalItems;
    private int     totalStoryPoints;

    // Breakdown by task_type (from dataset: bug=3.11, feature=8.01, enhancement=4.34)
    private int     featureCount;
    private int     bugCount;
    private int     enhancementCount;
    private double  avgSpByTaskType;

    // Risk breakdown
    private int     ambiguousItems;          // word_count ≤ 5, SP ≥ 5
    private int     highComplexityItems;     // num_components ≥ 5
    private int     highRiskItems;           // estimation_risk = HIGH
    private List<BacklogItemResponse> ambiguousItemList;

    // Estimated delivery
    private int     sprintsNeeded;           // totalSP / predictedVelocity
    private double  estimatedTotalHours;

    // Priority breakdown
    private Map<String, Integer> byPriority;
    private Map<String, Integer> byStatus;
}
