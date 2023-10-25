package uk.gov.hmcts.reform.em.hrs.ingestor.listener;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private DefaultIngestorService defaultIngestorService;

    @Value("${toggle.cronjob}")
    private boolean enableCronjob;

    static int secondsToAllowFlushingOfLogs = 200;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var client = new TelemetryClient();

        client.trackEvent("HRS Ingestor invoked");
        LOGGER.debug("HRS Ingestor invoked");
        LOGGER.debug("Enable Cronjob is set to {}", enableCronjob);
        LOGGER.info("defaultIngestorService.maxFilesToProcess: {}", defaultIngestorService.getMaxFilesToProcess());

        if (client != null && client.getContext() != null) {
            String ik = client.getContext().getInstrumentationKey();
            LOGGER.debug("Application Insights Key(4) = " + StringUtils.left(ik, 4));
            TelemetryContext context = client.getContext();
            ConcurrentMap<String, String> tags = context.getTags();
            tags.forEach((s, s2) -> LOGGER.info(s + ": " + s2));
            LOGGER.debug("context.getSession(): {}", context.getSession().toString());
            LOGGER.debug("context.getLocation(): {}", context.getLocation().toString());
        } else {
            LOGGER.info("No Application Insights Key");
        }


        if (enableCronjob) {
            try {
                LOGGER.debug("Application Started {}\n...About to Ingest", event);
                defaultIngestorService.ingest();
            } catch (Exception e) {
                flushLogs(client);
                LOGGER.error("Unhandled Exception during Ingestion - Aborted ... {}");
                throw new IngestorExecutionException("Error Intialising or Running Ingestor", e);
            }
            LOGGER.info("Initial Ingestion Complete", event);

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
            e.printStackTrace();
        }
    }
}
