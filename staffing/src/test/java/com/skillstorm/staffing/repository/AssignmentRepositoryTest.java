package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Assignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AssignmentRepositoryTest {

    @Autowired
    private AssignmentRepository assignmentRepository;

    private Assignment newAssignment(Long consultantId, Long engagementId, boolean active, boolean statusOverridden) {
        Assignment assignment = new Assignment(consultantId, engagementId, "Associate",
                LocalDate.of(2026, 1, 1));
        assignment.setAssignmentEndDate(LocalDate.of(2026, 6, 1));
        assignment.setActive(active);
        assignment.setStatusOverridden(statusOverridden);
        return assignmentRepository.save(assignment);
    }

    @Test
    void findByConsultantIdAndActiveTrue_returnsOnlyActiveAssignmentsForConsultant() {
        newAssignment(1L, 10L, true, false);
        newAssignment(1L, 11L, false, false);
        newAssignment(2L, 10L, true, false);

        List<Assignment> results = assignmentRepository.findByConsultantIdAndActiveTrue(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEngagementId()).isEqualTo(10L);
    }

    @Test
    void findByEngagementIdAndActiveTrue_returnsOnlyActiveAssignmentsForEngagement() {
        newAssignment(1L, 10L, true, false);
        newAssignment(2L, 10L, false, false);

        List<Assignment> results = assignmentRepository.findByEngagementIdAndActiveTrue(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getConsultantId()).isEqualTo(1L);
    }

    @Test
    void findByEngagementId_returnsFullHistoryIncludingInactive() {
        newAssignment(1L, 10L, true, false);
        newAssignment(2L, 10L, false, false);

        List<Assignment> results = assignmentRepository.findByEngagementId(10L);

        assertThat(results).hasSize(2);
    }

    @Test
    void findByEngagementIdAndActiveTrueAndStatusOverriddenFalse_excludesOverriddenAssignments() {
        newAssignment(1L, 10L, true, false);
        newAssignment(2L, 10L, true, true);

        List<Assignment> results = assignmentRepository.findByEngagementIdAndActiveTrueAndStatusOverriddenFalse(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getConsultantId()).isEqualTo(1L);
    }

    @Test
    void findByConsultantIdAndEngagementId_returnsMatchWhenExists() {
        newAssignment(1L, 10L, true, false);

        Optional<Assignment> result = assignmentRepository.findByConsultantIdAndEngagementId(1L, 10L);

        assertThat(result).isPresent();
    }

    @Test
    void findByConsultantIdAndEngagementId_returnsEmptyWhenNoMatch() {
        newAssignment(1L, 10L, true, false);

        Optional<Assignment> result = assignmentRepository.findByConsultantIdAndEngagementId(1L, 99L);

        assertThat(result).isEmpty();
    }
}
