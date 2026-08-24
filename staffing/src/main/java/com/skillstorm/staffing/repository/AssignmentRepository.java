package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByConsultantIdAndActiveTrue(Long consultantId);

    List<Assignment> findByEngagementIdAndActiveTrue(Long engagementId);

    List<Assignment> findByEngagementId(Long engagementId);

    List<Assignment> findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(Long engagementId);

    Optional<Assignment> findByConsultantIdAndEngagementId(Long consultantId, Long engagementId);

    boolean existsByConsultantIdAndEngagementIdAndActiveTrue(Long consultantId, Long engagementId);
}
