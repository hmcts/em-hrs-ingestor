package uk.gov.hmcts.reform.em.hrs.ingestor.listener;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.IngestorExecutionException;
import uk.gov.hmcts.reform.em.hrs.ingestor.service.DefaultIngestorService;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
public class IngestWhenApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestWhenApplicationReadyListener.class);

    private final DefaultIngestorService defaultIngestorService;

    private final boolean enableCronjob;

    static int secondsToAllowFlushingOfLogs = 200;

    @Autowired
    public IngestWhenApplicationReadyListener(DefaultIngestorService defaultIngestorService,
                                              @Value("${toggle.cronjob}") boolean enableCronjob
    ) {
        this.defaultIngestorService = defaultIngestorService;
        this.enableCronjob = enableCronjob;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var client = new TelemetryClient();

        client.trackEvent("HRS Ingestor invoked");
        LOGGER.debug("Enable Cronjob is set to {}", enableCronjob);
        LOGGER.info("defaultIngestorService.maxFilesToProcess: {}", defaultIngestorService.getMaxFilesToProcess());

        if (client.getContext() != null) {
            TelemetryContext context = client.getContext();
            ConcurrentMap<String, String> tags = context.getTags();
            tags.forEach((s, s2) -> LOGGER.info("{}:{} ", s, s2));
        } else {
            LOGGER.info("No Application Insights Key");
        }


        if (enableCronjob) {
            try {
                LOGGER.debug("Application Started {}\n...About to Ingest", event);
                defaultIngestorService.ingest();
            } catch (Exception e) {
                flushLogs(client);
                throw new IngestorExecutionException("Error Intialising or Running Ingestor", e);
            }
            LOGGER.info("Initial Ingestion Complete {}", event);

        } else {
            LOGGER.info("Application Not Starting as ENABLE_CRONJOB is false");
        }
        flushLogs(client);
        System.exit(0);
    }

    private void flushLogs(TelemetryClient client) {
        client.flush();
        try {
            TimeUnit.SECONDS.sleep(secondsToAllowFlushingOfLogs);
        } catch (InterruptedException e) {
            LOGGER.error("Waiting Flush logs failed ", e);
            Thread.currentThread().interrupt();
        }
    }
}
