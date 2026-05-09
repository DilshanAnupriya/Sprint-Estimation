package com.PMS.SP_Estimation.repo;

import com.PMS.SP_Estimation.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    List<TimeLog> findBySubTaskId(Long subTaskId);

    List<TimeLog> findByTeamMemberId(Long teamMemberId);
}
