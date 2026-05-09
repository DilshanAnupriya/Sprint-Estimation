package com.PMS.SP_Estimation.dto.request;

import com.PMS.SP_Estimation.entity.TeamMember;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamMemberRequest {
    @NotBlank
    private String name;
    private String email;
    private String role;
    private TeamMember.ExperienceLevel experienceLevel;
    private Double experienceYears;
}
