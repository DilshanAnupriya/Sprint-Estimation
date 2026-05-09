package com.PMS.SP_Estimation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A developer in the global pool. One developer can be assigned to many
 * projects (Jira-style); the {@link #projects} list is the inverse side of
 * the {@code project_team_members} join table owned by {@link Project}.
 */
@Entity(name = "team_members")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TeamMember {

    public enum ExperienceLevel {
        JUNIOR, MID, SENIOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String role;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    private Double experienceYears;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Relationships ────────────────────────────────────────────────────────
    // Inverse side of Project ↔ TeamMember M:N. Project owns the join table.

    @ManyToMany(mappedBy = "teamMembers", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    // SubTask.assignee and TimeLog.teamMember reference this entity.
    // No cascade — TeamMemberService.delete() handles cleanup explicitly.

    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<SubTask> assignedSubTasks = new ArrayList<>();

    @OneToMany(mappedBy = "teamMember", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<TimeLog> timeLogs = new ArrayList<>();
}
