package com.itq.service;

import com.itq.entity.ApprovalRegistry;
import com.itq.repository.ApprovalRegistryRepository;
import org.springframework.stereotype.Component;

@Component
public class ApprovalRegistryWriter {

    private final ApprovalRegistryRepository repository;

    public ApprovalRegistryWriter(ApprovalRegistryRepository repository) {
        this.repository = repository;
    }

    public void write(ApprovalRegistry registry) {
        repository.save(registry);
    }
}
