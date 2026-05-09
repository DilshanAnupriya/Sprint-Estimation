package com.PMS.SP_Estimation.dto.response;

import com.PMS.SP_Estimation.entity.TeamMember;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamMemberResponse {
    private Long    id;
    private String  name;
    private String  email;
    private String  role;
    private TeamMember.ExperienceLevel experienceLevel;
    private Double  experienceYears;

    /** Projects this developer is assigned to (Jira-style M:N). */
    private List<Long>   projectIds;
    private List<String> projectNames;
}
