package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SprintReadinessResponse {
    private Long    sprintId;
    private boolean isReady;

    // Checks based on dataset insights
    private boolean velocityPredicted;
    private boolean allItemsEstimated;      // all have story_points set
    private boolean noAmbiguousItems;       // no word_count ≤ 5 high-SP items
    private boolean capacityFeasible;       // totalSP ≤ recommendedCommitment
    private boolean teamAvailable;          // developer_availability ≥ 0.7
    private boolean noBurnoutRisk;

    private int     totalPlannedSP;
    private int     recommendedMax;
    private int     unestimatedCount;
    private List<String> blockers;          // reasons not ready
    private List<String> warnings;
}
