package com.PMS.SP_Estimation.repo;

import com.PMS.SP_Estimation.entity.Sprint;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectId(Long projectId);

    List<Sprint> findByProjectIdAndStatus(Long projectId, Sprint.Status status, Sort sort);
}
