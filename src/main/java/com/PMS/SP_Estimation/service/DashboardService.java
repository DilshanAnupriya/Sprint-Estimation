package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.response.BacklogItemResponse;
import com.PMS.SP_Estimation.dto.response.BurndownResponse;
import com.PMS.SP_Estimation.dto.response.ProjectSummaryResponse;
import com.PMS.SP_Estimation.dto.response.VelocityHistoryPoint;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.entity.Project;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.ProjectRepository;
import com.PMS.SP_Estimation.repo.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard data service — supplies burndown, velocity history,
 * risk items, and project summary for the frontend dashboard.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SprintRepository      sprintRepo;
    private final BacklogItemRepository backlogRepo;
    private final ProjectRepository     projectRepo;

    // ── Burndown ────────────────────────────────────────────────────

    /**
     * Returns a burndown snapshot for a sprint.
     * Groups all backlog items by status and aggregates story points.
     */
    public BurndownResponse getBurndown(Long sprintId) {
        Sprint sprint = sprintRepo.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        List<BacklogItem> items = backlogRepo.findBySprintId(sprintId);

        int totalSP = items.stream()
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int completedSP = items.stream()
            .filter(i -> i.getStatus() == BacklogItem.Status.DONE)
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();

        int completedItems  = (int) items.stream().filter(i -> i.getStatus() == BacklogItem.Status.DONE).count();
        int inProgressItems = (int) items.stream().filter(i -> i.getStatus() == BacklogItem.Status.IN_PROGRESS).count();
        int todoItems       = (int) items.stream().filter(i -> i.getStatus() == BacklogItem.Status.TODO).count();
        double completionRate = totalSP > 0 ? (double) completedSP / totalSP : 0.0;

        // Per-status breakdown
        Map<BacklogItem.Status, List<BacklogItem>> grouped = items.stream()
            .collect(Collectors.groupingBy(
                i -> i.getStatus() != null ? i.getStatus() : BacklogItem.Status.TODO));

        List<BurndownResponse.BurndownPoint> breakdown = grouped.entrySet().stream()
            .map(e -> BurndownResponse.BurndownPoint.builder()
                .status(e.getKey().name())
                .itemCount(e.getValue().size())
                .storyPoints(e.getValue().stream()
                    .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum())
                .build())
            .sorted(Comparator.comparing(BurndownResponse.BurndownPoint::getStatus))
            .toList();

        return BurndownResponse.builder()
            .sprintId(sprintId)
            .sprintName(sprint.getName())
            .totalItems(items.size())
            .totalStoryPoints(totalSP)
            .completedItems(completedItems)
            .completedStoryPoints(completedSP)
            .inProgressItems(inProgressItems)
            .todoItems(todoItems)
            .remainingStoryPoints(totalSP - completedSP)
            .completionRate(Math.round(completionRate * 1000.0) / 1000.0)
            .statusBreakdown(breakdown)
            .build();
    }

    // ── Velocity History ─────────────────────────────────────────────

    /**
     * Returns one data point per sprint (all statuses) sorted by end date.
     * Frontend uses this to draw the velocity trend line.
     */
    public List<VelocityHistoryPoint> getVelocityHistory(Long projectId) {
        if (!projectRepo.existsById(projectId))
            throw new ResourceNotFoundException("Project not found: " + projectId);

        return sprintRepo.findByProjectId(projectId).stream()
            .sorted(Comparator.comparing(Sprint::getEndDate,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .map(s -> VelocityHistoryPoint.builder()
                .sprintId(s.getId())
                .sprintName(s.getName())
                .predictedVelocity(s.getPredictedVelocity())
                .actualVelocity(s.getActualVelocity())
                .completionRate(s.getCompletionRate())
                .healthScore(s.getHealthScore())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .build())
            .toList();
    }

    // ── Risk Items ───────────────────────────────────────────────────

    /**
     * Returns backlog items in the sprint that have at least one risk flag:
     *  - estimation_risk = HIGH  (ML model flagged)
     *  - is_ambiguous = true     (short description + high complexity)
     *  - num_components ≥ 5      (high structural complexity)
     */
    public List<BacklogItemResponse> getHighRiskItems(Long sprintId) {
        if (!sprintRepo.existsById(sprintId))
            throw new ResourceNotFoundException("Sprint not found: " + sprintId);

        return backlogRepo.findBySprintId(sprintId).stream()
            .filter(i -> "HIGH".equals(i.getEstimationRisk())
                || Boolean.TRUE.equals(i.getIsAmbiguous())
                || (i.getNumComponents() != null && i.getNumComponents() >= 5))
            .map(this::toBacklogResponse)
            .toList();
    }

    // ── Project Summary ──────────────────────────────────────────────

    /**
     * Aggregates sprint + backlog stats into a single project summary card.
     */
    public ProjectSummaryResponse getProjectSummary(Long projectId) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        List<Sprint>      sprints = sprintRepo.findByProjectId(projectId);
        List<BacklogItem> items   = backlogRepo.findByProjectId(projectId);

        // Sprint counts
        long completedSprints = sprints.stream().filter(s -> s.getStatus() == Sprint.Status.COMPLETED).count();
        long activeSprints    = sprints.stream().filter(s -> s.getStatus() == Sprint.Status.ACTIVE).count();
        long plannedSprints   = sprints.stream().filter(s -> s.getStatus() == Sprint.Status.PLANNED).count();

        // Backlog counts
        long completed   = items.stream().filter(i -> i.getStatus() == BacklogItem.Status.DONE).count();
        long inProgress  = items.stream().filter(i -> i.getStatus() == BacklogItem.Status.IN_PROGRESS).count();
        long todo        = items.stream().filter(i -> i.getStatus() == BacklogItem.Status.TODO).count();
        long unestimated = items.stream().filter(i -> i.getStoryPoints() == null || i.getStoryPoints() == 0).count();
        long highRisk    = items.stream().filter(i -> "HIGH".equals(i.getEstimationRisk())).count();
        long ambiguous   = items.stream().filter(i -> Boolean.TRUE.equals(i.getIsAmbiguous())).count();

        int totalSP     = items.stream().mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int completedSP = items.stream()
            .filter(i -> i.getStatus() == BacklogItem.Status.DONE)
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();

        // Velocity from completed sprints
        List<Integer> velocities = sprints.stream()
            .filter(s -> s.getStatus() == Sprint.Status.COMPLETED && s.getActualVelocity() != null)
            .map(Sprint::getActualVelocity)
            .toList();
        double avgVelocity  = velocities.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double avgVelLast3  = velocities.stream()
            .skip(Math.max(0, velocities.size() - 3))
            .mapToInt(Integer::intValue).average().orElse(avgVelocity);

        double overallRate = totalSP > 0 ? (double) completedSP / totalSP : 0.0;

        return ProjectSummaryResponse.builder()
            .projectId(projectId)
            .projectName(project.getName())
            .domain(project.getDomain())
            .defaultTechStack(project.getDefaultTechStack())
            .teamSize(project.getTeamSize())
            .totalSprints(sprints.size())
            .completedSprints((int) completedSprints)
            .activeSprints((int) activeSprints)
            .plannedSprints((int) plannedSprints)
            .totalBacklogItems(items.size())
            .completedItems((int) completed)
            .inProgressItems((int) inProgress)
            .todoItems((int) todo)
            .totalStoryPoints(totalSP)
            .completedStoryPoints(completedSP)
            .unestimatedItems((int) unestimated)
            .highRiskItems((int) highRisk)
            .ambiguousItems((int) ambiguous)
            .avgActualVelocity(Math.round(avgVelocity * 10.0) / 10.0)
            .avgVelocityLast3(Math.round(avgVelLast3 * 10.0) / 10.0)
            .overallCompletionRate(Math.round(overallRate * 1000.0) / 1000.0)
            .build();
    }

    // ── Helper ───────────────────────────────────────────────────────

    private BacklogItemResponse toBacklogResponse(BacklogItem b) {
        return BacklogItemResponse.builder()
            .id(b.getId())
            .projectId(b.getProject() != null ? b.getProject().getId() : null)
            .sprintId(b.getSprint() != null ? b.getSprint().getId() : null)
            .title(b.getTitle())
            .userStory(b.getUserStory())
            .taskType(b.getTaskType())
            .priority(b.getPriority())
            .status(b.getStatus())
            .storyPoints(b.getStoryPoints())
            .mlPointEstimate(b.getMlPointEstimate())
            .mlLowerBound(b.getMlLowerBound())
            .mlUpperBound(b.getMlUpperBound())
            .estimationRisk(b.getEstimationRisk())
            .complexityScore(b.getComplexityScore())
            .isAmbiguous(b.getIsAmbiguous())
            .ambiguityReason(b.getAmbiguityReason())
            .numComponents(b.getNumComponents())
            .externalApis(b.getExternalApis())
            .createdAt(b.getCreatedAt())
            .updatedAt(b.getUpdatedAt())
            .build();
    }
}
