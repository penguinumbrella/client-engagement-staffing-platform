package com.skillstorm.staffing.repository;

import com.skillstorm.staffing.model.Consultant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultantRepository extends JpaRepository<Consultant, Long> {

    List<Consultant> findByActiveTrue();
}
