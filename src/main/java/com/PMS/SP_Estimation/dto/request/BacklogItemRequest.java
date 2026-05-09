package com.PMS.SP_Estimation.dto.request;

import com.PMS.SP_Estimation.entity.BacklogItem;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BacklogItemRequest {
    @NotBlank
    private String title;
    private String userStory;
    private BacklogItem.TaskType taskType;
    private BacklogItem.Priority priority;
    private BacklogItem.Status status;

    private String  techStack;
    private Integer numComponents;
    private Integer externalApis;
    private Boolean hasIntegration;
    private Boolean hasSecurity;
    private Boolean hasUiComplexity;

    private Integer storyPoints;
}
