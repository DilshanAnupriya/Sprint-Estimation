package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.response.SprintReadinessResponse;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SprintReadinessService {

    private final SprintRepository      sprintRepo;
    private final BacklogItemRepository backlogRepo;

    /**
     * Sprint readiness checklist — based on dataset insights.
     * Checks all preconditions before allowing sprint start.
     */
    public SprintReadinessResponse checkReadiness(Long sprintId) {
        Sprint sprint = sprintRepo.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));

        List<BacklogItem> items = backlogRepo.findBySprintId(sprintId);
        List<String> blockers     = new ArrayList<>();
        List<String> warnings     = new ArrayList<>();

        // Check 1: Velocity must be predicted
        boolean velPredicted = sprint.getPredictedVelocity() != null
                               && sprint.getPredictedVelocity() > 0;
        if (!velPredicted)
            blockers.add("No velocity prediction yet. Call POST /estimation/velocity first.");

        // Check 2: All items must have story points
        long unestimated = items.stream()
            .filter(i -> i.getStoryPoints() == null || i.getStoryPoints() == 0).count();
        boolean allEstimated = unestimated == 0;
        if (!allEstimated)
            blockers.add(unestimated + " backlog item(s) have no story point estimate.");

        // Check 3: No ambiguous stories with high SP
        // From dataset: 7.5% of stories are ambiguous — flag them before sprint
        long ambiguousCount = items.stream()
            .filter(i -> Boolean.TRUE.equals(i.getIsAmbiguous())).count();
        boolean noAmbiguous = ambiguousCount == 0;
        if (!noAmbiguous)
            warnings.add(ambiguousCount + " item(s) flagged as ambiguous (too vague). "
                + "Refine user story text before committing.");

        // Check 4: Capacity feasibility
        int totalSP      = items.stream()
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int recommended  = sprint.getPredictedVelocity() != null
            ? (int)(sprint.getPredictedVelocity() * 0.85) : 0;
        boolean feasible = totalSP <= recommended;
        if (!feasible && recommended > 0)
            warnings.add(String.format("Planned %d SP exceeds recommended capacity %d SP. "
                + "Consider moving %d SP to product backlog.", totalSP, recommended,
                totalSP - recommended));

        // Check 5: Team availability ≥ 0.70
        // From dataset: developer_availability mean=0.773, corr with velocity=0.232
        double avail = sprint.getDeveloperAvailability() != null
            ? sprint.getDeveloperAvailability() : 1.0;
        boolean teamAvail = avail >= 0.70;
        if (!teamAvail)
            warnings.add(String.format("Developer availability %.0f%% is below 70%% threshold. "
                + "Velocity prediction may be unreliable.", avail * 100));

        // Check 6: No burnout risk
        boolean noBurnout = !Boolean.TRUE.equals(sprint.getBurnoutRisk());
        if (!noBurnout)
            warnings.add("Burnout risk detected (high leave + carryover + bugs). "
                + "Consider reducing scope by 20%.");

        boolean ready = blockers.isEmpty();

        return SprintReadinessResponse.builder()
            .sprintId(sprintId)
            .isReady(ready)
            .velocityPredicted(velPredicted)
            .allItemsEstimated(allEstimated)
            .noAmbiguousItems(noAmbiguous)
            .capacityFeasible(feasible)
            .teamAvailable(teamAvail)
            .noBurnoutRisk(noBurnout)
            .totalPlannedSP(totalSP)
            .recommendedMax(recommended)
            .unestimatedCount((int) unestimated)
            .blockers(blockers)
            .warnings(warnings)
            .build();
    }
}
