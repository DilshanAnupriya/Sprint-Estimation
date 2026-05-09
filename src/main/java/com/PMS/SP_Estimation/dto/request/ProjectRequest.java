package com.PMS.SP_Estimation.dto.request;

import com.PMS.SP_Estimation.entity.TeamMember;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectRequest {
    @NotBlank
    private String name;
    private String description;
    private String domain;
    private String defaultTechStack;
    private Integer teamSize;
    private Integer defaultSprintDurationDays;

    // Estimation context — tell the AI what to expect for this team
    private Integer defaultTeamVelocity;      // SP/sprint the team typically delivers
    private Double  defaultCompletionRate;    // 0.0–1.0, typical sprint completion rate
    private TeamMember.ExperienceLevel defaultDevExperienceLevel;  // junior|mid|senior fallback
}
