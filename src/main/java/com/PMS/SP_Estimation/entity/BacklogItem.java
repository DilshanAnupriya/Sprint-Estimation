package com.PMS.SP_Estimation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "backlog_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BacklogItem {

    public enum TaskType {
        FEATURE, BUG, ENHANCEMENT, TECHNICAL_DEBT, RESEARCH
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 4000)
    private String userStory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Sprint sprint;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Raw dataset features used by the SP ML model
    private String  techStack;
    private Integer numComponents;
    private Integer externalApis;
    private Boolean hasIntegration;
    private Boolean hasSecurity;
    private Boolean hasUiComplexity;

    // ML estimation outputs
    private Integer storyPoints;
    private Integer mlPointEstimate;
    private Integer mlLowerBound;
    private Integer mlUpperBound;
    private String  estimationRisk;

    // Dataset-derived analytics
    private Double  complexityScore;
    private Double  estimatedHours;
    private Boolean isAmbiguous;
    private String  ambiguityReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Relationships (parent side) ──────────────────────────────────────────
    // Deleting a backlog item cascades to its subtasks, which cascade to their
    // time logs. Orphan removal keeps the collection in sync if items are
    // removed from the in-memory list.

    @OneToMany(mappedBy = "backlogItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<SubTask> subTasks = new ArrayList<>();
}
