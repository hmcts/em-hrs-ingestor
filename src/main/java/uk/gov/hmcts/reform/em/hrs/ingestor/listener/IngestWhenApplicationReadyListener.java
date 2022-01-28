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

@Component
public class IngestWhenApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestWhenApplicationReadyListener.class);
    @Value("${toggle.shutdown}")
    boolean shouldShutDownAfterInitialIngestion;
    @Autowired
    private DefaultIngestorService defaultIngestorService;
    @Value("${toggle.cronjob}")
    private boolean enableCronjob;
    @Autowired
    private TelemetryClient client;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        LOGGER.info("Enable Cronjob is set to {}", enableCronjob);
        LOGGER.info("defaultIngestorService.maxFilesToProcess: {}", defaultIngestorService.getMaxFilesToProcess());

        if (client != null && client.getContext() != null) {
            String ik = client.getContext().getInstrumentationKey();
            LOGGER.info("Application Insights Key(4) = " + StringUtils.left(ik, 4));
            TelemetryContext context = client.getContext();
            ConcurrentMap<String, String> tags = context.getTags();
            tags.forEach((s, s2) -> LOGGER.info(s + ": " + s2));
            LOGGER.info("context.getSession(): {}", context.getSession().getId());
            LOGGER.info("context..getUserAgent(): {}", context.getUser().getUserAgent());
            LOGGER.info("context.getOperation(): {}}", context.getOperation().getName());
            LOGGER.info("context.getLocation(): {}", context.getLocation().toString());


        } else {
            LOGGER.info("No Application Insights Key");
        }


        if (enableCronjob) {
            try {
                LOGGER.info("Application Started {}\n...About to Ingest", event);
                try {
                    defaultIngestorService.ingest();
                } catch (Exception e) {
                    throw new IngestorExecutionException("Error Intialising or Running Ingestor", e);
                }

                LOGGER.info("Initial Ingestion Complete", event);
            } catch (Exception e) {
                LOGGER.error("Unhandled Exception  during Ingestion - Aborted ... {}");
                e.printStackTrace();
            }

        } else {
            LOGGER.info("Application Not Starting as ENABLE_CRONJOB is false");
        }
        shutDownGracefully();
    }

    private void shutDownGracefully() {
        client.flush();

        if (shouldShutDownAfterInitialIngestion) {
            long millisToSleepForClientToFlush = 1000 * 10;
            try {
                Thread.sleep(millisToSleepForClientToFlush);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}
