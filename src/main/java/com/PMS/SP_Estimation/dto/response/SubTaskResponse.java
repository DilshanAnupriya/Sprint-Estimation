package com.PMS.SP_Estimation.dto.response;

import com.PMS.SP_Estimation.entity.SubTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubTaskResponse {
    private Long    id;
    private Long    backlogItemId;
    private String  backlogItemTitle;
    private Long    assigneeId;
    private String  assigneeName;
    private String  title;
    private String  description;
    private SubTask.Status status;
    private Double  estimatedHours;
    private Double  actualHours;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
