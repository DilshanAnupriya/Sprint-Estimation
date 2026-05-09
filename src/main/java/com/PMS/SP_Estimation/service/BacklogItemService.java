package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.BacklogItemRequest;
import com.PMS.SP_Estimation.dto.response.BacklogItemResponse;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BacklogItemService {

    private final BacklogItemRepository backlogRepo;
    private final ProjectRepository     projectRepo;
    private final SprintRepository      sprintRepo;

    public BacklogItemResponse create(Long projectId, BacklogItemRequest req) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        BacklogItem item = BacklogItem.builder()
            .project(project)
            .title(req.getTitle())
            .userStory(req.getUserStory())
            .taskType(req.getTaskType() != null ? req.getTaskType() : BacklogItem.TaskType.FEATURE)
            .priority(req.getPriority() != null ? req.getPriority() : BacklogItem.Priority.MEDIUM)
            .status(req.getStatus() != null ? req.getStatus() : BacklogItem.Status.TODO)
            .techStack(req.getTechStack())
            .numComponents(req.getNumComponents())
            .externalApis(req.getExternalApis())
            .hasIntegration(req.getHasIntegration())
            .hasSecurity(req.getHasSecurity())
            .hasUiComplexity(req.getHasUiComplexity())
            .storyPoints(req.getStoryPoints())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        return toResponse(backlogRepo.save(item));
    }

    public BacklogItemResponse update(Long id, BacklogItemRequest req) {
        BacklogItem item = find(id);
        if (req.getTitle() != null)            item.setTitle(req.getTitle());
        if (req.getUserStory() != null)        item.setUserStory(req.getUserStory());
        if (req.getTaskType() != null)         item.setTaskType(req.getTaskType());
        if (req.getPriority() != null)         item.setPriority(req.getPriority());
        if (req.getStatus() != null)           item.setStatus(req.getStatus());
        if (req.getTechStack() != null)        item.setTechStack(req.getTechStack());
        if (req.getNumComponents() != null)    item.setNumComponents(req.getNumComponents());
        if (req.getExternalApis() != null)     item.setExternalApis(req.getExternalApis());
        if (req.getHasIntegration() != null)   item.setHasIntegration(req.getHasIntegration());
        if (req.getHasSecurity() != null)      item.setHasSecurity(req.getHasSecurity());
        if (req.getHasUiComplexity() != null)  item.setHasUiComplexity(req.getHasUiComplexity());
        if (req.getStoryPoints() != null)      item.setStoryPoints(req.getStoryPoints());
        item.setUpdatedAt(LocalDateTime.now());
        return toResponse(backlogRepo.save(item));
    }

    @Transactional(readOnly = true)
    public BacklogItemResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<BacklogItemResponse> listByProject(Long projectId) {
        return backlogRepo.findByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BacklogItemResponse> listBySprint(Long sprintId) {
        return backlogRepo.findBySprintId(sprintId).stream().map(this::toResponse).toList();
    }

    public BacklogItemResponse assignToSprint(Long itemId, Long sprintId) {
        BacklogItem item = find(itemId);
        Sprint sprint = sprintRepo.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));
        item.setSprint(sprint);
        item.setUpdatedAt(LocalDateTime.now());
        return toResponse(backlogRepo.save(item));
    }

    public BacklogItemResponse unassignFromSprint(Long itemId) {
        BacklogItem item = find(itemId);
        item.setSprint(null);
        item.setUpdatedAt(LocalDateTime.now());
        return toResponse(backlogRepo.save(item));
    }

    public BacklogItemResponse markDone(Long itemId) {
        BacklogItem item = find(itemId);
        item.setStatus(BacklogItem.Status.DONE);
        item.setUpdatedAt(LocalDateTime.now());
        return toResponse(backlogRepo.save(item));
    }

    public void delete(Long id) {
        if (!backlogRepo.existsById(id))
            throw new ResourceNotFoundException("Backlog item not found: " + id);
        backlogRepo.deleteById(id);
    }

    private BacklogItem find(Long id) {
        return backlogRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Backlog item not found: " + id));
    }

    private BacklogItemResponse toResponse(BacklogItem b) {
        return BacklogItemResponse.builder()
            .id(b.getId())
            .projectId(b.getProject() != null ? b.getProject().getId() : null)
            .projectName(b.getProject() != null ? b.getProject().getName() : null)
            .sprintId(b.getSprint() != null ? b.getSprint().getId() : null)
            .sprintName(b.getSprint() != null ? b.getSprint().getName() : null)
            .subTaskCount(b.getSubTasks() != null ? b.getSubTasks().size() : 0)
            .title(b.getTitle())
            .userStory(b.getUserStory())
            .taskType(b.getTaskType())
            .priority(b.getPriority())
            .status(b.getStatus())
            .techStack(b.getTechStack())
            .numComponents(b.getNumComponents())
            .externalApis(b.getExternalApis())
            .hasIntegration(b.getHasIntegration())
            .hasSecurity(b.getHasSecurity())
            .hasUiComplexity(b.getHasUiComplexity())
            .storyPoints(b.getStoryPoints())
            .mlPointEstimate(b.getMlPointEstimate())
            .mlLowerBound(b.getMlLowerBound())
            .mlUpperBound(b.getMlUpperBound())
            .estimationRisk(b.getEstimationRisk())
            .complexityScore(b.getComplexityScore())
            .estimatedHours(b.getEstimatedHours())
            .isAmbiguous(b.getIsAmbiguous())
            .ambiguityReason(b.getAmbiguityReason())
            .createdAt(b.getCreatedAt())
            .updatedAt(b.getUpdatedAt())
            .build();
    }
}
