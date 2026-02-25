package com.itq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "document.workers")
@Setter
@Getter
public class WorkerProperties {
    private int batchSize = 50;
    private long submitIntervalMs = 5000;
    private long approveIntervalMs = 5000;
}
