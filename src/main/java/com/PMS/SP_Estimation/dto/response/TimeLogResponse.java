package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TimeLogResponse {
    private Long    id;
    private Long    subTaskId;
    private String  subTaskTitle;
    private Long    teamMemberId;
    private String  teamMemberName;
    private Double  hours;
    private LocalDate workDate;
    private String  description;
    private LocalDateTime createdAt;
}
