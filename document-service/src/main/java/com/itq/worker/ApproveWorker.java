package com.itq.worker;

import com.itq.config.WorkerProperties;
import com.itq.dto.BatchOpRequest;
import com.itq.dto.BatchResultDto;
import com.itq.dto.ItemResult;
import com.itq.entity.Document;
import com.itq.entity.enums.StatusChangeResultType;
import com.itq.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "document.workers.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ApproveWorker {

    private static final Logger log = LoggerFactory.getLogger(ApproveWorker.class);
    private static final String SYSTEM_INITIATOR = "approve-worker";

    private final DocumentService documentService;
    private final WorkerProperties workerProperties;

    @Scheduled(initialDelayString = "${document.workers.initial-delay-ms:10000}",
               fixedDelayString = "${document.workers.approve-interval-ms}")
    public void process() {
        int batchSize = workerProperties.getBatchSize();
        List<Document> batch = documentService.findSubmittedBatch(batchSize);

        if (batch.isEmpty()) {
            return;
        }

        List<Long> ids = batch.stream().map(Document::getId).toList();
        long start = System.currentTimeMillis();

        BatchResultDto result = documentService.approve(
            new BatchOpRequest(SYSTEM_INITIATOR, ids, null)
        );

        long elapsed = System.currentTimeMillis() - start;
        long success = result.results().stream()
            .filter(r -> r.result() == StatusChangeResultType.SUCCESS)
            .count();
        long failed = result.results().size() - success;

        log.info("APPROVE-worker: batch of {} documents processed in {} ms, success={}, failed={}",
            batch.size(), elapsed, success, failed);

        if (failed > 0) {
            List<ItemResult> errors = result.results().stream()
                .filter(r -> r.result() != StatusChangeResultType.SUCCESS)
                .toList();
            log.warn("APPROVE-worker: failed items: {}", errors);
        }
    }
}
