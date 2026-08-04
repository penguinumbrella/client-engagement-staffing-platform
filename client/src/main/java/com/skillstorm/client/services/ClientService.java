package com.skillstorm.client.services;

import java.util.List;

import com.skillstorm.client.models.clients;

public interface ClientService {

    List<clients> getAllClients();

    clients getClientById(Long id);

    clients createClient(clients client);

    clients updateClient(Long id, clients client);

    void deleteClient(Long id);

}
