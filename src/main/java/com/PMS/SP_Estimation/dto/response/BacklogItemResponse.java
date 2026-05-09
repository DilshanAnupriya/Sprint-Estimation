package com.PMS.SP_Estimation.dto.response;

import com.PMS.SP_Estimation.entity.BacklogItem;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BacklogItemResponse {
    private Long    id;
    private Long    projectId;
    private String  projectName;     // populated from Project relationship
    private Long    sprintId;
    private String  sprintName;      // populated from Sprint relationship (null if not in sprint)
    private Integer subTaskCount;    // populated from SubTask relationship
    private String  title;
    private String  userStory;
    private BacklogItem.TaskType taskType;
    private BacklogItem.Priority priority;
    private BacklogItem.Status   status;

    private String  techStack;
    private Integer numComponents;
    private Integer externalApis;
    private Boolean hasIntegration;
    private Boolean hasSecurity;
    private Boolean hasUiComplexity;

    private Integer storyPoints;
    private Integer mlPointEstimate;
    private Integer mlLowerBound;
    private Integer mlUpperBound;
    private String  estimationRisk;

    private Double  complexityScore;
    private Double  estimatedHours;
    private Boolean isAmbiguous;
    private String  ambiguityReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
