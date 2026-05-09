package com.PMS.SP_Estimation.dto.request;

import com.PMS.SP_Estimation.entity.SubTask;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubTaskRequest {
    @NotBlank
    private String title;
    private String description;
    private Long   assigneeId;
    private SubTask.Status status;
    private Double estimatedHours;
}
