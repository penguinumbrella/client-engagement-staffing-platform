package com.skillstorm.client.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.skillstorm.client.models.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Page<Client> findByIsActiveTrue(Pageable pageable);

}
