package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.ProjectRequest;
import com.PMS.SP_Estimation.dto.response.ProjectResponse;
import com.PMS.SP_Estimation.entity.Project;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;

    public ProjectResponse create(ProjectRequest req) {
        Project p = Project.builder()
            .name(req.getName())
            .description(req.getDescription())
            .domain(req.getDomain())
            .defaultTechStack(req.getDefaultTechStack())
            .teamSize(req.getTeamSize())
            .defaultSprintDurationDays(req.getDefaultSprintDurationDays())
            .defaultTeamVelocity(req.getDefaultTeamVelocity())
            .defaultCompletionRate(req.getDefaultCompletionRate())
            .defaultDevExperienceLevel(req.getDefaultDevExperienceLevel())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        return toResponse(projectRepo.save(p));
    }

    public ProjectResponse update(Long id, ProjectRequest req) {
        Project p = find(id);
        if (req.getName() != null)                      p.setName(req.getName());
        if (req.getDescription() != null)               p.setDescription(req.getDescription());
        if (req.getDomain() != null)                    p.setDomain(req.getDomain());
        if (req.getDefaultTechStack() != null)          p.setDefaultTechStack(req.getDefaultTechStack());
        if (req.getTeamSize() != null)                  p.setTeamSize(req.getTeamSize());
        if (req.getDefaultSprintDurationDays() != null) p.setDefaultSprintDurationDays(req.getDefaultSprintDurationDays());
        if (req.getDefaultTeamVelocity() != null)      p.setDefaultTeamVelocity(req.getDefaultTeamVelocity());
        if (req.getDefaultCompletionRate() != null)    p.setDefaultCompletionRate(req.getDefaultCompletionRate());
        if (req.getDefaultDevExperienceLevel() != null) p.setDefaultDevExperienceLevel(req.getDefaultDevExperienceLevel());
        p.setUpdatedAt(LocalDateTime.now());
        return toResponse(projectRepo.save(p));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepo.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Cascade-delete a project and everything underneath it:
     * sprints, backlog items (→ subtasks → time logs), team members.
     * Cascade rules on the entity collections handle the actual deletion.
     */
    @Transactional
    public void delete(Long id) {
        if (!projectRepo.existsById(id))
            throw new ResourceNotFoundException("Project not found: " + id);
        projectRepo.deleteById(id);
    }

    private Project find(Long id) {
        return projectRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private ProjectResponse toResponse(Project p) {
        // Counts via the bidirectional relationships — Hibernate lazy-loads
        // them inside the active transaction.
        int memberCount = p.getTeamMembers() != null ? p.getTeamMembers().size() : 0;
        int sprintCount = p.getSprints()      != null ? p.getSprints().size()      : 0;
        int itemCount   = p.getBacklogItems() != null ? p.getBacklogItems().size() : 0;

        return ProjectResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .description(p.getDescription())
            .domain(p.getDomain())
            .defaultTechStack(p.getDefaultTechStack())
            .teamSize(p.getTeamSize())
            .defaultSprintDurationDays(p.getDefaultSprintDurationDays())
            .defaultTeamVelocity(p.getDefaultTeamVelocity())
            .defaultCompletionRate(p.getDefaultCompletionRate())
            .defaultDevExperienceLevel(p.getDefaultDevExperienceLevel())
            .teamMemberCount(memberCount)
            .sprintCount(sprintCount)
            .backlogItemCount(itemCount)
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}
