package com.PMS.SP_Estimation.dto.response;

import com.PMS.SP_Estimation.entity.TeamMember;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long    id;
    private String  name;
    private String  description;
    private String  domain;
    private String  defaultTechStack;
    private Integer teamSize;
    private Integer defaultSprintDurationDays;

    // Counts populated from relationships (read-only, computed on read)
    private Integer teamMemberCount;
    private Integer sprintCount;
    private Integer backlogItemCount;
    private Integer defaultTeamVelocity;
    private Double  defaultCompletionRate;
    private TeamMember.ExperienceLevel defaultDevExperienceLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
