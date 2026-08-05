package com.skillstorm.client.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.skillstorm.client.models.clients;

public interface ClientRepository extends JpaRepository<clients, Long> {

    boolean existsByUserId(Long userId);
    Page<clients> findByUserId(Long userId, Pageable pageable);

}
