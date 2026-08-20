package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Consultant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultantRepository
        extends JpaRepository<Consultant, Long> {

    List<Consultant> findByActiveTrue();

    @Query(value = "SELECT * FROM consultants WHERE user_id = CAST(:userId AS uuid)", nativeQuery = true)
    Optional<Consultant> findByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT * FROM consultants WHERE user_id = CAST(:userId AS uuid) AND is_active = true", nativeQuery = true)
    Optional<Consultant> findByUserIdAndActiveTrue(@Param("userId") UUID userId);
}
