package com.skillstorm.client.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillstorm.client.models.clients;

public interface ClientRepository extends JpaRepository<clients, Long> {

    boolean existsByCompanyName(String companyName);

}
