package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Consultant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultantRepository
        extends JpaRepository<Consultant, Long> {

    List<Consultant> findByActiveTrue();

    Optional<Consultant> findByUserId(UUID userId);

    Optional<Consultant> findByUserIdAndActiveTrue(UUID userId);
}
