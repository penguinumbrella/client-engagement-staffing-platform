package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Consultant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;

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

        Consultant consultant = createConsultant();

        setField(consultant, "name", "Jane Doe");
        setField(consultant, "titleRole", "Senior Consultant");
        setField(consultant, "primarySkillArea", "Audit");
        setField(consultant, "userId", UUID.randomUUID());

        consultant.setActive(active);

        return consultantRepository.save(consultant);
    }

    private Consultant createConsultant() {
        try {
            Constructor<Consultant> constructor =
                    Consultant.class.getDeclaredConstructor();

            constructor.setAccessible(true);

            return constructor.newInstance();

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(
            Consultant consultant,
            String fieldName,
            Object value) {

        try {
            var field =
                    Consultant.class.getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(consultant, value);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findByActiveTrue_returnsOnlyActiveConsultants() {

        newConsultant(true);
        newConsultant(false);

        List<Consultant> results =
                consultantRepository.findByActiveTrue();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isActive()).isTrue();
    }

    @Test
    void findByActiveTrue_returnsEmptyWhenNoneActive() {

        newConsultant(false);

        assertThat(
                consultantRepository.findByActiveTrue()
        ).isEmpty();
    }

    @Test
    void findByActiveTrue_pageable_returnsActivePage() {
        newConsultant(true);
        newConsultant(true);
        newConsultant(false);

        Page<Consultant> page = consultantRepository.findByActiveTrue(PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).isActive()).isTrue();
    }

    @Test
    void save_persistsAndAssignsGeneratedId() {

        Consultant saved =
                newConsultant(true);

        assertThat(saved.getId()).isNotNull();

        assertThat(
                consultantRepository.findById(
                        saved.getId()
                )
        ).isPresent();
    }

    @Test
    void save_setsAuditTimestampsOnPersist() {

        Consultant saved =
                newConsultant(true);

        assertThat(saved.getCreatedAt())
                .isNotNull();

        assertThat(saved.getUpdatedAt())
                .isNotNull();
    }
}