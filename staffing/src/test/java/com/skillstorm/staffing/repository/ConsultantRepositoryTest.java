package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Consultant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConsultantRepositoryTest {

    @Autowired
    private ConsultantRepository consultantRepository;

    private Consultant newConsultant(boolean active) {
        Consultant consultant = new Consultant("Jane Doe", "Senior Consultant", "Audit");
        consultant.setActive(active);
        return consultantRepository.save(consultant);
    }

    @Test
    void findByActiveTrue_returnsOnlyActiveConsultants() {
        newConsultant(true);
        newConsultant(false);

        List<Consultant> results = consultantRepository.findByActiveTrue();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isActive()).isTrue();
    }

    @Test
    void findByActiveTrue_returnsEmptyWhenNoneActive() {
        newConsultant(false);

        assertThat(consultantRepository.findByActiveTrue()).isEmpty();
    }

    @Test
    void save_persistsAndAssignsGeneratedId() {
        Consultant saved = newConsultant(true);

        assertThat(saved.getId()).isNotNull();
        assertThat(consultantRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void save_setsAuditTimestampsOnPersist() {
        Consultant saved = newConsultant(true);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
