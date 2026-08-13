package com.skillstorm.engagement.repository;

import com.skillstorm.engagement.model.Engagement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EngagementRepositoryTest {

    @Autowired
    private EngagementRepository engagementRepository;

    private Engagement newEngagement(Long clientId, String status, boolean active) {
        Engagement engagement = new Engagement(
                "Audit Rollout", clientId, "Audit",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), status);
        engagement.setActive(active);
        return engagementRepository.save(engagement);
    }

    @Test
    void findByActiveTrue_returnsOnlyActiveEngagements() {
        newEngagement(1L, "Planned", true);
        newEngagement(1L, "Cancelled", false);

        List<Engagement> results = engagementRepository.findByActiveTrue();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isActive()).isTrue();
    }

    @Test
    void findByClientIdAndActiveTrue_filtersByClientAndActiveFlag() {
        newEngagement(1L, "Planned", true);
        newEngagement(2L, "Planned", true);
        newEngagement(1L, "Cancelled", false);

        List<Engagement> results = engagementRepository.findByClientIdAndActiveTrue(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClientId()).isEqualTo(1L);
    }

    @Test
    void findByClientIdAndActiveTrue_returnsEmptyWhenNoMatch() {
        newEngagement(1L, "Planned", true);

        List<Engagement> results = engagementRepository.findByClientIdAndActiveTrue(999L);

        assertThat(results).isEmpty();
    }

    @Test
    void save_persistsAndAssignsGeneratedId() {
        Engagement saved = newEngagement(1L, "Planned", true);

        assertThat(saved.getId()).isNotNull();
        assertThat(engagementRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void save_setsAuditTimestampsOnPersist() {
        Engagement saved = newEngagement(1L, "Planned", true);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
