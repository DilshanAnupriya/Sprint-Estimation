package com.PMS.SP_Estimation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "sub_tasks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SubTask {

    public enum Status {
        TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backlog_item_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private BacklogItem backlogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TeamMember assignee;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Double estimatedHours;
    private Double actualHours;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // ─── Relationships (parent side) ──────────────────────────────────────────
    // Deleting a subtask cascades to its time logs (the "audit" of work done).

    @OneToMany(mappedBy = "subTask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<TimeLog> timeLogs = new ArrayList<>();
}
