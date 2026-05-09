package com.PMS.SP_Estimation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SprintRequest {
    @NotBlank
    private String name;
    private String techStack;            // optional — overrides project.defaultTechStack for this sprint
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private Integer predictedVelocity;
    private Double  developerAvailability;
    private Integer leaveDaysTotal;
}
