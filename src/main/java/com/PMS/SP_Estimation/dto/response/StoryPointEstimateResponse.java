package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoryPointEstimateResponse {
    private Long    backlogItemId;
    private Integer pointEstimate;
    private Integer lowerBound;
    private Integer upperBound;
    private String  riskLevel;
    private Double  rawPoint;
    private Double  rawP20;
    private Double  rawP80;
    private Boolean usedFineTuned;
    private String  domainUsed;

    // Actual values used by the ML model (override > item field > keyword-inferred).
    // The frontend can display these so users know what the model saw.
    private String  usedTechStack;
    private String  usedDomain;
    private String  usedTaskType;
    private String  usedDevExperienceLevel;
    private Integer usedTeamSize;
    private Integer usedSprintDurationDays;
    private Double  usedRecentCompletionRate;
    private Integer usedSimilarTaskCount;
    private Integer usedTaskAgeDays;
    private Integer usedTeamVelocityAvg;
    private Boolean usedHasIntegration;
    private Boolean usedHasSecurity;
    private Boolean usedHasUiComplexity;
    private Integer usedNumComponents;
    private Integer usedExternalApis;
    private String  usedUserStory;
}
