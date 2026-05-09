package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.SubTaskRequest;
import com.PMS.SP_Estimation.dto.response.SubTaskResponse;
import com.PMS.SP_Estimation.entity.BacklogItem;
import com.PMS.SP_Estimation.entity.SubTask;
import com.PMS.SP_Estimation.entity.TeamMember;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.BacklogItemRepository;
import com.PMS.SP_Estimation.repo.SubTaskRepository;
import com.PMS.SP_Estimation.repo.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubTaskService {

    private final SubTaskRepository    subTaskRepo;
    private final BacklogItemRepository backlogRepo;
    private final TeamMemberRepository teamRepo;

    public SubTaskResponse create(Long backlogItemId, SubTaskRequest req) {
        BacklogItem backlog = backlogRepo.findById(backlogItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Backlog item not found: " + backlogItemId));
        TeamMember assignee = req.getAssigneeId() == null ? null
            : teamRepo.findById(req.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found: " + req.getAssigneeId()));

        SubTask t = SubTask.builder()
            .backlogItem(backlog)
            .title(req.getTitle())
            .description(req.getDescription())
            .assignee(assignee)
            .status(req.getStatus() != null ? req.getStatus() : SubTask.Status.TODO)
            .estimatedHours(req.getEstimatedHours())
            .actualHours(0.0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        return toResponse(subTaskRepo.save(t));
    }

    public SubTaskResponse update(Long id, SubTaskRequest req) {
        SubTask t = find(id);
        if (req.getTitle() != null)          t.setTitle(req.getTitle());
        if (req.getDescription() != null)    t.setDescription(req.getDescription());
        if (req.getEstimatedHours() != null) t.setEstimatedHours(req.getEstimatedHours());
        if (req.getStatus() != null)         applyStatus(t, req.getStatus());
        if (req.getAssigneeId() != null) {
            TeamMember a = teamRepo.findById(req.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found: " + req.getAssigneeId()));
            t.setAssignee(a);
        }
        t.setUpdatedAt(LocalDateTime.now());
        return toResponse(subTaskRepo.save(t));
    }

    public SubTaskResponse get(Long id) {
        return toResponse(find(id));
    }

    public List<SubTaskResponse> listByBacklog(Long backlogItemId) {
        return subTaskRepo.findByBacklogItemId(backlogItemId).stream().map(this::toResponse).toList();
    }

    public List<SubTaskResponse> listByAssignee(Long assigneeId) {
        return subTaskRepo.findByAssigneeId(assigneeId).stream().map(this::toResponse).toList();
    }

    /** Developers update status — also auto-marks the parent story DONE when all subtasks finish. */
    public SubTaskResponse changeStatus(Long id, SubTask.Status newStatus) {
        SubTask t = find(id);
        applyStatus(t, newStatus);
        t.setUpdatedAt(LocalDateTime.now());
        SubTask saved = subTaskRepo.save(t);
        cascadeParentStatus(saved.getBacklogItem());
        return toResponse(saved);
    }

    public void delete(Long id) {
        if (!subTaskRepo.existsById(id))
            throw new ResourceNotFoundException("Sub-task not found: " + id);
        subTaskRepo.deleteById(id);
    }

    /** Recompute actual hours from logged time entries. Called by TimeLogService. */
    public void recomputeActualHours(Long subTaskId, double totalHours) {
        SubTask t = find(subTaskId);
        t.setActualHours(Math.round(totalHours * 100.0) / 100.0);
        t.setUpdatedAt(LocalDateTime.now());
        subTaskRepo.save(t);
    }

    private void applyStatus(SubTask t, SubTask.Status s) {
        t.setStatus(s);
        if (s == SubTask.Status.DONE && t.getCompletedAt() == null) {
            t.setCompletedAt(LocalDateTime.now());
        } else if (s != SubTask.Status.DONE) {
            t.setCompletedAt(null);
        }
    }

    private void cascadeParentStatus(BacklogItem parent) {
        if (parent == null) return;
        List<SubTask> siblings = subTaskRepo.findByBacklogItemId(parent.getId());
        if (siblings.isEmpty()) return;
        boolean allDone = siblings.stream().allMatch(s -> s.getStatus() == SubTask.Status.DONE);
        boolean anyInProgress = siblings.stream().anyMatch(s ->
            s.getStatus() == SubTask.Status.IN_PROGRESS || s.getStatus() == SubTask.Status.IN_REVIEW);

        if (allDone && parent.getStatus() != BacklogItem.Status.DONE) {
            parent.setStatus(BacklogItem.Status.DONE);
            parent.setUpdatedAt(LocalDateTime.now());
            backlogRepo.save(parent);
        } else if (!allDone && anyInProgress && parent.getStatus() == BacklogItem.Status.TODO) {
            parent.setStatus(BacklogItem.Status.IN_PROGRESS);
            parent.setUpdatedAt(LocalDateTime.now());
            backlogRepo.save(parent);
        }
    }

    private SubTask find(Long id) {
        return subTaskRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sub-task not found: " + id));
    }

    private SubTaskResponse toResponse(SubTask t) {
        return SubTaskResponse.builder()
            .id(t.getId())
            .backlogItemId(t.getBacklogItem() != null ? t.getBacklogItem().getId() : null)
            .backlogItemTitle(t.getBacklogItem() != null ? t.getBacklogItem().getTitle() : null)
            .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
            .assigneeName(t.getAssignee() != null ? t.getAssignee().getName() : null)
            .title(t.getTitle())
            .description(t.getDescription())
            .status(t.getStatus())
            .estimatedHours(t.getEstimatedHours())
            .actualHours(t.getActualHours())
            .createdAt(t.getCreatedAt())
            .completedAt(t.getCompletedAt())
            .build();
    }
}
