package com.PMS.SP_Estimation.dto.request;

import com.PMS.SP_Estimation.entity.TeamMember;
import lombok.Data;

/**
 * Optional overrides for the SP estimate endpoint.
 *
 * Mirrors the Jupyter notebook's interactive prompts so the frontend can
 * collect every ML input from the user instead of relying on project state
 * (which may be empty on a brand-new project).
 *
 * Any non-null field overrides the value derived from the project / backlog
 * item. Anything left null falls back to the auto-derived value.
 */
@Data
public class EstimationOverrideRequest {
    // Categorical
    private String  domain;                  // ecommerce | finance | healthcare | education | logistics | <new>
    private String  techStack;               // springboot | nodejs | react | django | dotnet | flask | vue | angular | fastapi | rails
    private String  taskType;                // feature | bug | enhancement | technical_debt | research — overrides item.taskType
    private TeamMember.ExperienceLevel devExperienceLevel;

    // Team / sprint context
    private Integer teamSize;
    private Integer sprintDurationDays;
    private Integer teamVelocityAvg;         // SP/sprint
    private Double  recentCompletionRate;    // 0.0–1.0
    private Integer similarTaskCount;
    private Integer taskAgeDays;

    // Story features (override entity fields if you want a what-if estimate
    // without first updating the BacklogItem)
    private Integer numComponents;
    private Integer externalApis;
    private Boolean hasIntegration;
    private Boolean hasSecurity;
    private Boolean hasUiComplexity;
    private String  userStory;
}
