package com.PMS.SP_Estimation.repo;

import com.PMS.SP_Estimation.entity.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

    List<SubTask> findByBacklogItemId(Long backlogItemId);

    List<SubTask> findByAssigneeId(Long assigneeId);
}
