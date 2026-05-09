package com.PMS.SP_Estimation.service.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class MLClient {

    private final WebClient mlWebClient;

    public StoryPointPrediction predictStoryPoint(StoryPointPredictRequest req) {
        return mlWebClient.post()
            .uri("/predict/story-point")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .bodyToMono(StoryPointPrediction.class)
            .block();
    }

    public VelocityPrediction predictVelocity(VelocityPredictRequest req) {
        return mlWebClient.post()
            .uri("/predict/velocity")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .bodyToMono(VelocityPrediction.class)
            .block();
    }

    // ── DTOs matching the FastAPI schemas ──────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryPointPredictRequest {
        @JsonProperty("user_story")             private String  userStory;
        @JsonProperty("task_type")              private String  taskType;
        @JsonProperty("domain")                 private String  domain;
        @JsonProperty("tech_stack")             private String  techStack;
        @JsonProperty("dev_experience_level")   private String  devExperienceLevel;
        @JsonProperty("num_components")         private Integer numComponents;
        @JsonProperty("external_apis")          private Integer externalApis;
        @JsonProperty("has_integration")        private Integer hasIntegration;
        @JsonProperty("has_security")           private Integer hasSecurity;
        @JsonProperty("has_ui_complexity")      private Integer hasUiComplexity;
        @JsonProperty("team_size")              private Integer teamSize;
        @JsonProperty("sprint_duration_days")   private Integer sprintDurationDays;
        @JsonProperty("similar_task_count")     private Integer similarTaskCount;
        @JsonProperty("team_velocity_avg")      private Integer teamVelocityAvg;
        @JsonProperty("recent_completion_rate") private Double  recentCompletionRate;
        @JsonProperty("task_age_days")          private Integer taskAgeDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryPointPrediction {
        @JsonProperty("point_estimate")   private Integer pointEstimate;
        @JsonProperty("lower_bound")      private Integer lowerBound;
        @JsonProperty("upper_bound")      private Integer upperBound;
        @JsonProperty("risk_level")       private String  riskLevel;
        @JsonProperty("raw_point")        private Double  rawPoint;
        @JsonProperty("raw_p20")          private Double  rawP20;
        @JsonProperty("raw_p80")          private Double  rawP80;
        @JsonProperty("used_fine_tuned")  private Boolean usedFineTuned;
        @JsonProperty("domain_used")      private String  domainUsed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityPredictRequest {
        @JsonProperty("avg_velocity_last3")      private Double  avgVelocityLast3;
        @JsonProperty("avg_velocity_last5")      private Double  avgVelocityLast5;
        @JsonProperty("team_size")               private Integer teamSize;
        @JsonProperty("sprint_duration_days")    private Integer sprintDurationDays;
        @JsonProperty("completion_rate")         private Double  completionRate;
        @JsonProperty("carryover_tasks")         private Integer carryoverTasks;
        @JsonProperty("developer_availability")  private Double  developerAvailability;
        @JsonProperty("leave_days")              private Integer leaveDays;
        @JsonProperty("planned_story_points")    private Integer plannedStoryPoints;
        @JsonProperty("workload_ratio")          private Double  workloadRatio;
        @JsonProperty("num_bugs")                private Integer numBugs;
        @JsonProperty("avg_experience_years")    private Double  avgExperienceYears;
        @JsonProperty("domain")                  private String  domain;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityPrediction {
        @JsonProperty("predicted_velocity")  private Integer predictedVelocity;
        @JsonProperty("raw_prediction")      private Double  rawPrediction;
        @JsonProperty("base_prediction")     private Integer basePrediction;
        @JsonProperty("used_fine_tuned")     private Boolean usedFineTuned;
        @JsonProperty("domain_used")         private String  domainUsed;
        @JsonProperty("risk_level")          private String  riskLevel;
        @JsonProperty("stress_score")        private Double  stressScore;
        @JsonProperty("velocity_delta")      private Double  velocityDelta;
        @JsonProperty("effective_capacity")  private Double  effectiveCapacity;
        @JsonProperty("velocity_per_dev")    private Double  velocityPerDev;
    }
}
