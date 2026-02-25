package com.itq.service;

import com.itq.dto.BatchOpRequest;
import com.itq.dto.DocumentCreateRequest;
import com.itq.entity.enums.StatusChangeResultType;
import com.itq.exception.RegistryException;
import com.itq.repository.ApprovalRegistryRepository;
import com.itq.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class ApproveRegistryRollbackTest {

    @Autowired
    DocumentService documentService;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    ApprovalRegistryRepository approvalRegistryRepository;

    @MockBean
    ApprovalRegistryWriter approvalRegistryWriter;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        approvalRegistryRepository.deleteAll();
    }

    @Test
    void approve_rollbackWhenRegistryFails() {
        Long id = documentService.createDocument(new DocumentCreateRequest("initiator", "Author", "Title"));
        documentService.submit(new BatchOpRequest("user", List.of(id), null));

        doThrow(new RegistryException("Registry write failed")).when(approvalRegistryWriter).write(any());

        var result = documentService.approve(new BatchOpRequest("user", List.of(id), null));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).result()).isEqualTo(StatusChangeResultType.REGISTRY_ERROR);

        var doc = documentRepository.findById(id).orElseThrow();
        assertThat(doc.getStatus().name()).isEqualTo("SUBMITTED");

        assertThat(approvalRegistryRepository.existsByDocument_Id(id)).isFalse();
    }
}
