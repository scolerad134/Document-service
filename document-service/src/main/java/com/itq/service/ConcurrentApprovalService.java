package com.itq.service;

import com.itq.dto.ConcurrentApprovalResult;
import com.itq.entity.enums.StatusChangeResultType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ConcurrentApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentApprovalService.class);

    private final DocumentService documentService;

    public ConcurrentApprovalResult runConcurrentApproval(Long documentId, int threads, int attempts, String initiator) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        int totalAttempts = threads * attempts;
        List<Future<StatusChangeResultType>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            for (int a = 0; a < attempts; a++) {
                futures.add(executor.submit(() -> {
                    try {
                        documentService.approveOne(documentId, initiator, "concurrent test");
                        successCount.incrementAndGet();
                        return StatusChangeResultType.SUCCESS;
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("must be in SUBMITTED")) {
                            conflictCount.incrementAndGet();
                            return StatusChangeResultType.CONFLICT;
                        }
                        if (msg != null && msg.contains("not found")) {
                            return StatusChangeResultType.NOT_FOUND;
                        }
                        errorCount.incrementAndGet();
                        return StatusChangeResultType.REGISTRY_ERROR;
                    }
                }));
            }
        }

        for (Future<StatusChangeResultType> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Task failed: {}", e.getMessage());
                errorCount.incrementAndGet();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var doc = documentService.getDocument(documentId);
        String finalStatus = doc.status().name();

        String message = "Expected: exactly 1 success, rest conflicts. " +
            "Success=" + successCount.get() + ", Conflict=" + conflictCount.get() + ", Error=" + errorCount.get();

        return new ConcurrentApprovalResult(
            documentId,
            totalAttempts,
            successCount.get(),
            conflictCount.get(),
            errorCount.get(),
            finalStatus,
            message
        );
    }
}
