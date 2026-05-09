package com.PMS.SP_Estimation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "projects")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;

    private String domain;
    private String defaultTechStack;

    private Integer teamSize;
    private Integer defaultSprintDurationDays;

    private Integer defaultTeamVelocity;
    private Double  defaultCompletionRate;

    @Enumerated(EnumType.STRING)
    private TeamMember.ExperienceLevel defaultDevExperienceLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Relationships (parent side) ──────────────────────────────────────────
    // Cascade ALL + orphanRemoval so deleting a project cleans up its children.
    // @JsonIgnore prevents infinite recursion when the entity is serialized directly.
    // Controllers return DTOs anyway, so these collections never appear in API output.

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Sprint> sprints = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<BacklogItem> backlogItems = new ArrayList<>();

    /**
     * Many-to-Many with TeamMember (developer pool). A developer can be on
     * many projects (Jira-style). Deleting a project removes the join rows
     * but leaves the developer alive — they may be on other projects.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_team_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "team_member_id")
    )
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<TeamMember> teamMembers = new ArrayList<>();
}
