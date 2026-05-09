package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.SprintCompletionRequest;
import com.PMS.SP_Estimation.dto.request.SprintMetricsRequest;
import com.PMS.SP_Estimation.dto.request.SprintRequest;
import com.PMS.SP_Estimation.dto.response.SprintResponse;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.entity.Project;
import com.PMS.SP_Estimation.entity.Sprint;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.ProjectRepository;
import com.PMS.SP_Estimation.repo.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository      sprintRepo;
    private final ProjectRepository     projectRepo;
    private final BacklogItemRepository backlogRepo;
    private final VelocityAnalyticsService velocityAnalytics;

    public SprintResponse create(Long projectId, SprintRequest req) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        Integer duration = req.getDurationDays();
        if (duration == null && req.getStartDate() != null && req.getEndDate() != null) {
            duration = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate());
        }
        if (duration == null) duration = project.getDefaultSprintDurationDays() != null
            ? project.getDefaultSprintDurationDays() : 14;

        // Seed rolling velocity inputs (used by the ML model on /estimation/velocity)
        Map<String, Double> rolling = velocityAnalytics.computeRollingVelocities(projectId);

        // Tech stack: sprint-specific override > project default
        String techStack = req.getTechStack() != null && !req.getTechStack().isBlank()
            ? req.getTechStack() : project.getDefaultTechStack();

        Sprint s = Sprint.builder()
            .project(project)
            .name(req.getName())
            .techStack(techStack)
            .status(Sprint.Status.PLANNED)
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .durationDays(duration)
            .predictedVelocity(req.getPredictedVelocity())
            .developerAvailability(req.getDeveloperAvailability())
            .leaveDaysTotal(req.getLeaveDaysTotal())
            .avgVelocityLast3(rolling.get("avg_velocity_last3"))
            .avgVelocityLast5(rolling.get("avg_velocity_last5"))
            .build();
        return toResponse(sprintRepo.save(s));
    }

    public SprintResponse update(Long id, SprintRequest req) {
        Sprint s = find(id);
        if (req.getName() != null)                  s.setName(req.getName());
        if (req.getTechStack() != null)             s.setTechStack(req.getTechStack());
        if (req.getStartDate() != null)             s.setStartDate(req.getStartDate());
        if (req.getEndDate() != null)               s.setEndDate(req.getEndDate());
        if (req.getDurationDays() != null)          s.setDurationDays(req.getDurationDays());
        if (req.getPredictedVelocity() != null)     s.setPredictedVelocity(req.getPredictedVelocity());
        if (req.getDeveloperAvailability() != null) s.setDeveloperAvailability(req.getDeveloperAvailability());
        if (req.getLeaveDaysTotal() != null)        s.setLeaveDaysTotal(req.getLeaveDaysTotal());
        return toResponse(sprintRepo.save(s));
    }

    @Transactional(readOnly = true)
    public SprintResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> listByProject(Long projectId) {
        return sprintRepo.findByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    /**
     * Delete a sprint without losing its backlog items.
     * Detaches every assigned BacklogItem (sets sprint=null) before removing
     * the sprint, so items return to the project's product backlog instead of
     * being deleted alongside the sprint.
     */
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        if (!sprintRepo.existsById(id))
            throw new ResourceNotFoundException("Sprint not found: " + id);
        for (BacklogItem item : backlogRepo.findBySprintId(id)) {
            item.setSprint(null);
            backlogRepo.save(item);
        }
        sprintRepo.deleteById(id);
    }

    /** Begin the sprint — locks scope. */
    public SprintResponse start(Long id) {
        Sprint s = find(id);
        if (s.getStatus() == Sprint.Status.COMPLETED)
            throw new IllegalStateException("Sprint is already completed");
        s.setStatus(Sprint.Status.ACTIVE);
        return toResponse(sprintRepo.save(s));
    }

    /** Update SV-style metrics during the sprint (availability, bugs, leave). */
    public SprintResponse updateMetrics(Long id, SprintMetricsRequest req) {
        Sprint s = find(id);
        if (req.getDeveloperAvailability() != null) s.setDeveloperAvailability(req.getDeveloperAvailability());
        if (req.getLeaveDaysTotal() != null)        s.setLeaveDaysTotal(req.getLeaveDaysTotal());
        if (req.getNumBugsThisSprint() != null)     s.setNumBugsThisSprint(req.getNumBugsThisSprint());
        if (req.getCarryoverTasks() != null)        s.setCarryoverTasks(req.getCarryoverTasks());
        if (req.getCompletionRate() != null)        s.setCompletionRate(req.getCompletionRate());
        return toResponse(sprintRepo.save(s));
    }

    /**
     * End-of-sprint settlement.
     *  - actualVelocity ← sum of SP from DONE backlog items in the sprint (or override)
     *  - completionRate ← actualVelocity / plannedSP (or override)
     *  - carryoverTasks ← count of non-DONE items (or override)
     *  - status         ← COMPLETED
     * Then refreshes rolling velocities on the project so the next sprint
     * starts with up-to-date avg_last3 / avg_last5.
     */
    public SprintResponse complete(Long id, SprintCompletionRequest req) {
        Sprint s = find(id);
        if (s.getStatus() == Sprint.Status.COMPLETED)
            throw new IllegalStateException("Sprint is already completed");

        List<BacklogItem> items = backlogRepo.findBySprintId(id);
        int plannedSP = items.stream()
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int doneSP = items.stream()
            .filter(i -> i.getStatus() == BacklogItem.Status.DONE)
            .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int carryover = (int) items.stream()
            .filter(i -> i.getStatus() != BacklogItem.Status.DONE && i.getStatus() != BacklogItem.Status.CANCELLED)
            .count();

        int actualVelocity = req != null && req.getActualVelocity() != null
            ? req.getActualVelocity() : doneSP;
        double completionRate = req != null && req.getCompletionRate() != null
            ? req.getCompletionRate()
            : plannedSP > 0 ? (double) actualVelocity / plannedSP : 0.0;
        int carryoverTasks = req != null && req.getCarryoverTasks() != null
            ? req.getCarryoverTasks() : carryover;
        int numBugs = req != null && req.getNumBugsThisSprint() != null
            ? req.getNumBugsThisSprint()
            : (s.getNumBugsThisSprint() != null ? s.getNumBugsThisSprint() : 0);

        s.setActualVelocity(actualVelocity);
        s.setCompletionRate(Math.round(completionRate * 1000.0) / 1000.0);
        s.setCarryoverTasks(carryoverTasks);
        s.setNumBugsThisSprint(numBugs);
        s.setStatus(Sprint.Status.COMPLETED);
        sprintRepo.save(s);

        // Refresh rolling velocities for the project (next-sprint inputs)
        if (s.getProject() != null) {
            Map<String, Double> rolling = velocityAnalytics.computeRollingVelocities(s.getProject().getId());
            s.setAvgVelocityLast3(rolling.get("avg_velocity_last3"));
            s.setAvgVelocityLast5(rolling.get("avg_velocity_last5"));
            sprintRepo.save(s);
        }
        return toResponse(s);
    }

    private Sprint find(Long id) {
        return sprintRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + id));
    }

    private SprintResponse toResponse(Sprint s) {
        // Pull counts via the bidirectional relationship — cheap because the
        // sprint is already loaded inside the same persistence context.
        var items = s.getBacklogItems();
        int itemCount = items != null ? items.size() : 0;
        int plannedSP = items == null ? 0 : items.stream()
            .mapToInt(b -> b.getStoryPoints() != null ? b.getStoryPoints() : 0).sum();

        return SprintResponse.builder()
            .id(s.getId())
            .projectId(s.getProject() != null ? s.getProject().getId() : null)
            .projectName(s.getProject() != null ? s.getProject().getName() : null)
            .backlogItemCount(itemCount)
            .totalPlannedSP(plannedSP)
            .name(s.getName())
            .techStack(s.getTechStack())
            .status(s.getStatus())
            .startDate(s.getStartDate())
            .endDate(s.getEndDate())
            .durationDays(s.getDurationDays())
            .predictedVelocity(s.getPredictedVelocity())
            .actualVelocity(s.getActualVelocity())
            .healthScore(s.getHealthScore())
            .velocityTrend(s.getVelocityTrend())
            .effectiveCapacity(s.getEffectiveCapacity())
            .isOvercommitted(s.getIsOvercommitted())
            .burnoutRisk(s.getBurnoutRisk())
            .velocityPerDev(s.getVelocityPerDev())
            .completionRate(s.getCompletionRate())
            .carryoverTasks(s.getCarryoverTasks())
            .numBugsThisSprint(s.getNumBugsThisSprint())
            .leaveDaysTotal(s.getLeaveDaysTotal())
            .developerAvailability(s.getDeveloperAvailability())
            .avgVelocityLast3(s.getAvgVelocityLast3())
            .avgVelocityLast5(s.getAvgVelocityLast5())
            .build();
    }
}
