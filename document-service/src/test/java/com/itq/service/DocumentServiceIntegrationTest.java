package com.itq.service;

import com.itq.dto.*;
import com.itq.entity.enums.StatusChangeResultType;
import com.itq.repository.ApprovalRegistryRepository;
import com.itq.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DocumentServiceIntegrationTest {

    @Autowired
    DocumentService documentService;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    ApprovalRegistryRepository approvalRegistryRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        approvalRegistryRepository.deleteAll();
    }

    @Test
    void createDocument_happyPath() {
        var req = new DocumentCreateRequest("user@test", "Author1", "Test Doc");
        Long id = documentService.createDocument(req);

        assertThat(id).isNotNull();

        var doc = documentService.getDocument(id);
        assertThat(doc.author()).isEqualTo("Author1");
        assertThat(doc.title()).isEqualTo("Test Doc");
        assertThat(doc.status().name()).isEqualTo("DRAFT");
        assertThat(doc.uniqueNumber()).isNotNull();
    }

    @Test
    void submit_singleDocument() {
        Long id = documentService.createDocument(new DocumentCreateRequest("user", "A", "T"));
        var result = documentService.submit(new BatchOpRequest("user1", java.util.List.of(id), null));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).result()).isEqualTo(StatusChangeResultType.SUCCESS);

        var doc = documentService.getDocument(id);
        assertThat(doc.status().name()).isEqualTo("SUBMITTED");

        var withHistory = documentService.getDocumentWithHistory(id);
        assertThat(withHistory.history()).hasSize(1);
        assertThat(withHistory.history().get(0).action()).isEqualTo("SUBMIT");
    }

    @Test
    void approve_singleDocument() {
        Long id = documentService.createDocument(new DocumentCreateRequest("user", "A", "T"));
        documentService.submit(new BatchOpRequest("u1", java.util.List.of(id), null));

        var result = documentService.approve(new BatchOpRequest("u2", java.util.List.of(id), "OK"));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).result()).isEqualTo(StatusChangeResultType.SUCCESS);

        var doc = documentService.getDocument(id);
        assertThat(doc.status().name()).isEqualTo("APPROVED");

        assertThat(approvalRegistryRepository.existsByDocument_Id(id)).isTrue();
    }

    @Test
    void submit_batch_partialResults() {
        Long id1 = documentService.createDocument(new DocumentCreateRequest("user", "A", "T1"));
        Long id2 = documentService.createDocument(new DocumentCreateRequest("user", "B", "T2"));
        Long id3 = 99999L; // не существует

        documentService.submit(new BatchOpRequest("u1", java.util.List.of(id1), null));
        // id1 уже SUBMITTED, id2 DRAFT, id3 не найден

        var result = documentService.submit(
            new BatchOpRequest("u1", java.util.List.of(id1, id2, id3), null)
        );

        assertThat(result.results()).hasSize(3);
        assertThat(result.results().stream().filter(r -> r.result() == StatusChangeResultType.SUCCESS).count()).isEqualTo(1);
        assertThat(result.results().stream().filter(r -> r.result() == StatusChangeResultType.CONFLICT).count()).isEqualTo(1);
        assertThat(result.results().stream().filter(r -> r.result() == StatusChangeResultType.NOT_FOUND).count()).isEqualTo(1);
    }

    @Test
    void approve_batch_partialResults() {
        Long id1 = documentService.createDocument(new DocumentCreateRequest("user", "A", "T1"));
        Long id2 = documentService.createDocument(new DocumentCreateRequest("user", "B", "T2"));
        Long id3 = 99999L;

        documentService.submit(new BatchOpRequest("u1", java.util.List.of(id1, id2), null));
        documentService.approve(new BatchOpRequest("u1", java.util.List.of(id1), null));
        // id1 APPROVED, id2 SUBMITTED, id3 не найден

        var result = documentService.approve(
            new BatchOpRequest("u1", java.util.List.of(id1, id2, id3), null)
        );

        assertThat(result.results()).hasSize(3);
        assertThat(result.results().stream().filter(r -> r.result() == StatusChangeResultType.SUCCESS).count()).isEqualTo(1);
        assertThat(result.results().stream().filter(r -> r.result() == StatusChangeResultType.CONFLICT).count()).isEqualTo(1);
        assertThat(result.results().stream().filter(r -> r.result() == StatusChangeResultType.NOT_FOUND).count()).isEqualTo(1);
    }

}
