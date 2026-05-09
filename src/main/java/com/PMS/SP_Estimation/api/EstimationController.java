package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.request.EstimationOverrideRequest;
import com.PMS.SP_Estimation.dto.request.SprintPlanRequest;
import com.PMS.SP_Estimation.dto.request.VelocityPredictionRequest;
import com.PMS.SP_Estimation.dto.response.SprintPlanResponse;
import com.PMS.SP_Estimation.dto.response.StoryPointEstimateResponse;
import com.PMS.SP_Estimation.dto.response.VelocityPredictionResponse;
import com.PMS.SP_Estimation.service.SprintPlanService;
import com.PMS.SP_Estimation.service.StoryPointMLService;
import com.PMS.SP_Estimation.service.VelocityMLService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Estimation endpoints — direct entry-points for both ML models.
 *
 * Story-point endpoint:   POST /api/v1/estimation/story-points/{backlogItemId}
 *   Calls the SP XGBoost + quantile ensemble (transfer-learning adapter).
 *   Persists ml_* fields on the backlog item; sets story_points if currently null.
 *
 * Velocity endpoint:      POST /api/v1/estimation/velocity
 *   Calls the velocity XGBoost + domain adapter.
 *   All fields are optional — any null falls back to sprint / project state.
 *   Persists predicted_velocity on the sprint when sprint_id is supplied.
 */
@RestController
@RequestMapping("/api/v1/estimation")
@RequiredArgsConstructor
public class EstimationController {

    private final StoryPointMLService spMLService;
    private final VelocityMLService   velMLService;
    private final SprintPlanService   planService;

    // ── Story Point Prediction ────────────────────────────────────────

    /**
     * Predict story points for the given backlog item.
     * The service auto-builds the full feature row from the item + project state.
     *
     * @param itemId  ID of the BacklogItem to estimate
     * @return point_estimate, lower/upper bounds (Fibonacci), risk_level, raw scores
     */
    @PostMapping("/story-points/{itemId}")
    public ResponseEntity<StoryPointEstimateResponse> estimateStoryPoints(
            @PathVariable Long itemId,
            @RequestBody(required = false) EstimationOverrideRequest overrides) {
        return ResponseEntity.ok(spMLService.estimate(itemId, overrides));
    }

    // ── Velocity Prediction ──────────────────────────────────────────

    /**
     * Predict next-sprint velocity.
     *
     * Supply sprint_id to let the service load team / project context automatically.
     * Any field left null falls back to the sprint's stored metrics or project defaults.
     * You may also call this with a fully explicit body (no sprint_id) for what-if
     * planning before a sprint is created.
     *
     * @param req  VelocityPredictionRequest (all fields optional when sprint_id given)
     * @return predicted_velocity, risk_level, stress_score, recommended_commitment …
     */
    @PostMapping("/velocity")
    public ResponseEntity<VelocityPredictionResponse> predictVelocity(
            @RequestBody @Valid VelocityPredictionRequest req) {
        return ResponseEntity.ok(velMLService.predict(req));
    }

    // ── Combined Sprint Plan ──────────────────────────────────────────

    /**
     * All-in-one sprint planning endpoint.
     *
     * 1. Predicts next-sprint velocity (same as POST /velocity).
     * 2. If estimateAll=true, runs the SP ML model on every unestimated
     *    backlog item in the sprint and persists the results.
     * 3. Returns a feasibility summary: plannedSP vs recommendedMax,
     *    surplus / headroom, and actionable warnings.
     *
     * @param req  SprintPlanRequest — supply sprintId for full context;
     *             all velocity fields are optional (null → auto-derived).
     */
    @PostMapping("/sprint-plan")
    public ResponseEntity<SprintPlanResponse> planSprint(
            @RequestBody @Valid SprintPlanRequest req) {
        return ResponseEntity.ok(planService.plan(req));
    }
}
