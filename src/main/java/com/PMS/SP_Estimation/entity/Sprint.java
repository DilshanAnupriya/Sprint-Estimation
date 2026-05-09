package com.PMS.SP_Estimation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "sprints")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Sprint {

    public enum Status {
        PLANNED, ACTIVE, COMPLETED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project project;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;

    /** Optional sprint-specific tech stack — falls back to project.defaultTechStack when null. */
    private String techStack;

    private Integer predictedVelocity;
    private Integer actualVelocity;

    // Dataset-derived fields
    private Double  healthScore;
    private Double  velocityTrend;
    private Double  effectiveCapacity;
    private Boolean isOvercommitted;
    private Boolean burnoutRisk;
    private Double  velocityPerDev;
    private Double  completionRate;
    private Integer carryoverTasks;
    private Integer numBugsThisSprint;
    private Integer leaveDaysTotal;
    private Double  developerAvailability;
    private Double  avgVelocityLast3;
    private Double  avgVelocityLast5;

    // ─── Relationships (back-side) ────────────────────────────────────────────
    // No cascade: deleting a sprint must NOT delete its backlog items — they
    // belong to the project, and should simply detach (sprint=null).
    // SprintService.delete() handles the detachment explicitly.

    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<BacklogItem> backlogItems = new ArrayList<>();
}
