package com.PMS.SP_Estimation.service;

import com.PMS.SP_Estimation.dto.request.TeamMemberRequest;
import com.PMS.SP_Estimation.dto.response.TeamMemberResponse;
import com.PMS.SP_Estimation.entity.Project;
import com.PMS.SP_Estimation.entity.SubTask;
import com.PMS.SP_Estimation.entity.TeamMember;
import com.PMS.SP_Estimation.exception.ResourceNotFoundException;
import com.PMS.SP_Estimation.repo.ProjectRepository;
import com.PMS.SP_Estimation.repo.SubTaskRepository;
import com.PMS.SP_Estimation.repo.TeamMemberRepository;
import com.PMS.SP_Estimation.repo.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamRepo;
    private final ProjectRepository    projectRepo;
    private final SubTaskRepository    subTaskRepo;
    private final TimeLogRepository    timeLogRepo;

    /**
     * Add a developer to a project. If a developer with the same email already
     * exists in the global pool, that record is reused and merely linked to the
     * project — otherwise a fresh developer is created and linked.
     * (Jira-style: the dev pool is global, projects share developers.)
     */
    @Transactional
    public TeamMemberResponse create(Long projectId, TeamMemberRequest req) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        TeamMember member = (req.getEmail() != null && !req.getEmail().isBlank())
            ? teamRepo.findByEmail(req.getEmail()).orElse(null) : null;

        if (member == null) {
            member = TeamMember.builder()
                .name(req.getName())
                .email(req.getEmail())
                .role(req.getRole())
                .experienceLevel(req.getExperienceLevel())
                .experienceYears(req.getExperienceYears())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            member = teamRepo.save(member);
        }

        // Link via the project-owned join table
        if (!project.getTeamMembers().contains(member)) {
            project.getTeamMembers().add(member);
            projectRepo.save(project);
        }
        return toResponse(member);
    }

    /** Assign an existing developer (from the global pool) to a project. */
    @Transactional
    public TeamMemberResponse assignToProject(Long projectId, Long memberId) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        TeamMember member = find(memberId);
        if (!project.getTeamMembers().contains(member)) {
            project.getTeamMembers().add(member);
            projectRepo.save(project);
        }
        return toResponse(member);
    }

    /** Remove a developer from a project (does NOT delete the developer). */
    @Transactional
    public void removeFromProject(Long projectId, Long memberId) {
        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        TeamMember member = find(memberId);
        project.getTeamMembers().remove(member);
        projectRepo.save(project);
    }

    public TeamMemberResponse update(Long id, TeamMemberRequest req) {
        TeamMember m = find(id);
        if (req.getName() != null)             m.setName(req.getName());
        if (req.getEmail() != null)            m.setEmail(req.getEmail());
        if (req.getRole() != null)             m.setRole(req.getRole());
        if (req.getExperienceLevel() != null)  m.setExperienceLevel(req.getExperienceLevel());
        if (req.getExperienceYears() != null)  m.setExperienceYears(req.getExperienceYears());
        m.setUpdatedAt(LocalDateTime.now());
        return toResponse(teamRepo.save(m));
    }

    @Transactional(readOnly = true)
    public TeamMemberResponse get(Long id) {
        return toResponse(find(id));
    }

    /** List developers assigned to a specific project. */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listByProject(Long projectId) {
        return teamRepo.findByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    /** List every developer in the global pool (Jira-style). */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listAll() {
        return teamRepo.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Permanently delete a developer from the global pool.
     * Unassigns them from every project (join rows), null-outs subtask
     * assignments, and deletes their time logs first to avoid FK violations.
     */
    @Transactional
    public void delete(Long id) {
        TeamMember member = find(id);

        // Unlink from every project
        for (Project p : List.copyOf(member.getProjects())) {
            p.getTeamMembers().remove(member);
            projectRepo.save(p);
        }

        // Detach from any subtasks they were assigned to
        for (SubTask st : subTaskRepo.findByAssigneeId(id)) {
            st.setAssignee(null);
            subTaskRepo.save(st);
        }

        // Delete their time logs (work record)
        timeLogRepo.findByTeamMemberId(id).forEach(timeLogRepo::delete);

        teamRepo.deleteById(id);
    }

    private TeamMember find(Long id) {
        return teamRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team member not found: " + id));
    }

    private TeamMemberResponse toResponse(TeamMember m) {
        var projects = m.getProjects();
        return TeamMemberResponse.builder()
            .id(m.getId())
            .name(m.getName())
            .email(m.getEmail())
            .role(m.getRole())
            .experienceLevel(m.getExperienceLevel())
            .experienceYears(m.getExperienceYears())
            .projectIds(projects == null ? List.of()
                : projects.stream().map(Project::getId).toList())
            .projectNames(projects == null ? List.of()
                : projects.stream().map(Project::getName).toList())
            .build();
    }
}
