package uk.gov.hmcts.reform.em.hrs.ingestor.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.IngestorExecutionException;
import uk.gov.hmcts.reform.em.hrs.ingestor.service.DefaultIngestorService;

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

        LOGGER.info("HRS Ingestor invoked");
        LOGGER.info("Enable Cronjob is set to {}", enableCronjob);
        LOGGER.info("defaultIngestorService.maxFilesToProcess: {}", defaultIngestorService.getMaxFilesToProcess());


        if (enableCronjob) {
            try {
                LOGGER.info("Application Started {}\n...About to Ingest", event);
                defaultIngestorService.ingest();
            } catch (Exception e) {
                LOGGER.error("Unhandled Exception during Ingestion - Aborted ... {}");
                throw new IngestorExecutionException("Error Intialising or Running Ingestor", e);
            }
            LOGGER.info("Initial Ingestion Complete", event);

        } else {
            LOGGER.info("Application Not Starting as ENABLE_CRONJOB is false");
        }
        System.exit(0);

    }

}
