package com.PMS.SP_Estimation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TimeLogRequest {
    @NotNull
    private Long   teamMemberId;
    @NotNull
    @Positive
    private Double hours;
    private LocalDate workDate;
    private String description;
}
