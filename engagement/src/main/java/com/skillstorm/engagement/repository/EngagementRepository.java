package com.skillstorm.engagement.repository;

import com.skillstorm.engagement.model.Engagement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EngagementRepository extends JpaRepository<Engagement, Long> {

    List<Engagement> findByActiveTrue();

    List<Engagement> findByClientIdAndActiveTrue(Long clientId);
}
