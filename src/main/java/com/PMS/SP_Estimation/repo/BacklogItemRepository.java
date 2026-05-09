package com.PMS.SP_Estimation.repo;

import com.PMS.SP_Estimation.entity.BacklogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BacklogItemRepository extends JpaRepository<BacklogItem, Long> {

    List<BacklogItem> findByProjectId(Long projectId);

    List<BacklogItem> findBySprintId(Long sprintId);

    /**
     * Sum of story_points for DONE items in a sprint.
     * Used by SprintService.complete() and burndown calculations.
     */
    @Query("SELECT COALESCE(SUM(b.storyPoints), 0) FROM backlog_items b " +
           "WHERE b.sprint.id = :sprintId AND b.status = 'DONE'")
    int sumCompletedPointsBySprint(@Param("sprintId") Long sprintId);

    /**
     * Count of items per status for a sprint — used to build burndown data.
     * Returns Object[] rows of [status, count, sumStoryPoints].
     */
    @Query("SELECT b.status, COUNT(b), COALESCE(SUM(b.storyPoints), 0) " +
           "FROM backlog_items b WHERE b.sprint.id = :sprintId GROUP BY b.status")
    List<Object[]> getBurndownData(@Param("sprintId") Long sprintId);
}
