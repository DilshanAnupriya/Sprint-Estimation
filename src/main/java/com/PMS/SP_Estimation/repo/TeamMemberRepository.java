package com.PMS.SP_Estimation.repo;

import com.PMS.SP_Estimation.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /** Developers assigned to this project (via project_team_members join table). */
    @Query("SELECT m FROM team_members m JOIN m.projects p WHERE p.id = :projectId")
    List<TeamMember> findByProjectId(@Param("projectId") Long projectId);

    /** Count of developers assigned to this project. */
    @Query("SELECT COUNT(m) FROM team_members m JOIN m.projects p WHERE p.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    /** Lookup by email — used when creating a developer to detect duplicates in the global pool. */
    Optional<TeamMember> findByEmail(String email);
}
