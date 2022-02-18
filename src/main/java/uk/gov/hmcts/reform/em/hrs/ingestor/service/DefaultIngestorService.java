package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.HrsApiException;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsApiClient;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.CvpBlobstoreClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DefaultIngestorService implements IngestorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIngestorService.class);
    private static int itemsAttempted;
    private static int filesParsedOk;
    private static int itemsIgnoredOk;
    private static int filesSubmittedOk;

    private static int cvpFilesCountTotal;
    private static int hrsFileCountTotal;
    private static int filesToIngestCountTotal;


    private final CvpBlobstoreClient cvpBlobstoreClient;
    private final HrsApiClient hrsApiClient;
    private final IngestionFilterer ingestionFilterer;
    private final MetadataResolver metadataResolver;

    @Setter
    @Getter
    @Value("${ingestion.max-files-to-process}")
    private Integer maxFilesToProcess = 100;

    @Autowired
    public DefaultIngestorService(final CvpBlobstoreClient cvpBlobstoreClient,
                                  final HrsApiClient hrsApiClient,
                                  final IngestionFilterer ingestionFilterer,
                                  final MetadataResolver metadataResolver) {
        this.cvpBlobstoreClient = cvpBlobstoreClient;
        this.hrsApiClient = hrsApiClient;
        this.ingestionFilterer = ingestionFilterer;
        this.metadataResolver = metadataResolver;
    }

    private static void resetCounters() {
        itemsAttempted = 0;
        filesParsedOk = 0;
        itemsIgnoredOk = 0;
        filesSubmittedOk = 0;

        cvpFilesCountTotal = 0;
        hrsFileCountTotal = 0;
        filesToIngestCountTotal = 0;

    }

    private static void tallyItemsAttempted() {
        itemsAttempted++;
    }

    private static void tallyFilesSubmittedOk() {
        filesSubmittedOk++;
    }

    private static void tallyFilesParsedOk() {
        filesParsedOk++;
    }

    private static void tallyItemsIgnored() {
        itemsIgnoredOk++;
    }

    @Override
    public void ingest() {
        ingest(maxFilesToProcess);
    }

    private void ingest(Integer maxNumberOfFiles) {
        resetCounters();
        LOGGER.info("Ingestion Started with BATCH PROCESSING LIMIT of {}", maxNumberOfFiles);
        final Set<String> foldersSet = cvpBlobstoreClient.getFolders();
        List<String> folders = foldersSet.stream().collect(Collectors.toList());
        Collections.shuffle(folders);

        LOGGER.info("Folders found in CVP {} ", folders.size());
        folders.forEach(folder -> {

            LOGGER.info("--------------------------------------------");
            LOGGER.info("Inspecting folder: {}", folder);
            final Set<CvpItem> filteredSet = getFilesToIngest(folder);
            LOGGER.debug("filterSet size: {}", filteredSet.size());
            filteredSet.forEach(file -> {
                if (!batchProcessingLimitReached(maxNumberOfFiles)) {
                    tallyItemsAttempted();
                    resolveMetaDataAndPostFileToHrs(file);
                }
            });
            LOGGER.info("Running Total of Files Attempted: {}", itemsAttempted);

        });
        LOGGER.info("Ingestion Complete");
        if (batchProcessingLimitReached(maxNumberOfFiles)) {
            LOGGER.info("Batch Processing Limit Reached ({})", maxNumberOfFiles);
        }
        LOGGER.info("Total files Attempted: {}", itemsAttempted);
        LOGGER.info("Total files Parsed Ok: {}", filesParsedOk);
        LOGGER.info("Total files Ignored Ok: {}", itemsIgnoredOk);
        LOGGER.info("Total files Submitted Ok: {}", filesSubmittedOk);


        String ingestionStatus = determineFolderStatus(filesToIngestCountTotal);
        LOGGER.info("VALIDATION REPORT: CVP Files:{}, HRS Files:{}, To Ingest:{}, INGESTION-STATUS:{}",
                    cvpFilesCountTotal, hrsFileCountTotal, filesToIngestCountTotal, ingestionStatus
        );

    }

    private void resolveMetaDataAndPostFileToHrs(CvpItem cvpItem) {
        try {
            LOGGER.debug("Resolving Filename {}", cvpItem.getFilename());
            final Metadata metaData = metadataResolver.resolve(cvpItem);
            if (metaData == null) {
                tallyItemsIgnored();
                return;
            }
            tallyFilesParsedOk();
            hrsApiClient.postFile(metaData);
            tallyFilesSubmittedOk();
        } catch (FilenameParsingException fp) {
            LOGGER.error(
                "Unable to parse filename: {} => {}",
                cvpItem.getFilename(),
                fp.getMessage()
            );
        } catch (HrsApiException hrsApi) {
            LOGGER.error(
                "HRS API Response error: {} => {} => {}",
                hrsApi.getCode(),
                hrsApi.getMessage(),
                hrsApi.getBody()
            );
        } catch (Exception e) {
            LOGGER.error(
                "Exception processing cvpItem Filename::{}",
                cvpItem.getFilename(),
                e
            ); // TODO: covered by EM-3582
        }
    }

    private boolean batchProcessingLimitReached(Integer maxNumberOfFiles) {
        return itemsAttempted >= maxNumberOfFiles;
    }

    private Set<CvpItem> getFilesToIngest(final String folder) {
        try {
            LOGGER.debug("Getting CVP files in folder");
            final CvpItemSet cvpItemSet = cvpBlobstoreClient.findByFolder(folder);
            LOGGER.debug("Getting HRS files already ingested");
            final HrsFileSet hrsFileSet = hrsApiClient.getIngestedFiles(folder);
            LOGGER.debug("Filtering out files not required from original cvp list");
            Set<CvpItem> filesToIngest = ingestionFilterer.filter(cvpItemSet, hrsFileSet);

            int cvpFilesCount = cvpItemSet.getCvpItems().size();
            int hrsFileCount = hrsFileSet.getHrsFiles().size();
            int filesToIngestCount = filesToIngest.size();

            tallyCvpFilesCountTotal(cvpFilesCount);
            tallyHrsFilesCountTotal(hrsFileCount);
            tallyFilesToIngestCount(filesToIngestCount);


            String ingestionStatus = determineFolderStatus(filesToIngestCount);

            LOGGER.info("Folder:{}, CVP Files:{}, HRS Files:{}, To Ingest:{}, FOLDER-STATUS:{}",
                        folder, cvpFilesCount, hrsFileCount, filesToIngestCount, ingestionStatus
            );
            return filesToIngest;
        } catch (HrsApiException | IOException e) {
            LOGGER.error("", e); // TODO: covered by EM-3582
            return Collections.emptySet();
        }
    }

    @NotNull
    private String determineFolderStatus(int filesToIngestCount) {
        return filesToIngestCount == 0 ? "COMPLETE" : "PENDING";
    }

    private static void tallyFilesToIngestCount(int filesToIngestCount) {
        filesToIngestCountTotal += filesToIngestCount;
    }

    private static void tallyHrsFilesCountTotal(int hrsFileCount) {
        hrsFileCountTotal += hrsFileCount;
    }

    private static void tallyCvpFilesCountTotal(int cvpFilesCount) {
        cvpFilesCountTotal += cvpFilesCount;
    }

}
