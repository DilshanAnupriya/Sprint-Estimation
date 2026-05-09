package com.PMS.SP_Estimation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "time_logs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_task_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SubTask subTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TeamMember teamMember;

    private Double hours;
    private LocalDate workDate;

    @Column(length = 1000)
    private String description;

    private LocalDateTime createdAt;
}
