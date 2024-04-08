package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.HrsApiException;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsApiClient;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.BlobstoreClientHelper;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.VhBlobstoreClientHelper;

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

    private final BlobstoreClientHelper cvpBlobstoreHelper;
    private final VhBlobstoreClientHelper vhBlobstoreClient;
    private final HrsApiClient hrsApiClient;
    private final IngestionFilterer ingestionFilterer;
    private final MetadataResolver metadataResolver;

    @Setter
    @Getter
    @Value("${ingestion.max-files-to-process}")
    private Integer maxFilesToProcess = 100;

    private int vhProcessCount;
    private boolean vhProcess;
    private boolean cvpProcess;

    @Autowired
    public DefaultIngestorService(
        final @Qualifier("cvpBlobstoreClientHelper") BlobstoreClientHelper cvpBlobstoreClient,
        final VhBlobstoreClientHelper vhBlobstoreClient,
        final HrsApiClient hrsApiClient,
        final IngestionFilterer ingestionFilterer,
        final MetadataResolver metadataResolver,
        @Value("${ingestion.vh.max-blob-process-count}") int vhProcessCount,
        @Value("${ingestion.vh.process}") boolean vhProcess,
        @Value("${ingestion.cvp.process}") boolean cvpProcess
    ) {
        this.cvpBlobstoreHelper = cvpBlobstoreClient;
        this.vhBlobstoreClient = vhBlobstoreClient;
        this.hrsApiClient = hrsApiClient;
        this.ingestionFilterer = ingestionFilterer;
        this.metadataResolver = metadataResolver;
        this.vhProcessCount = vhProcessCount;
        this.vhProcess = vhProcess;
        this.cvpProcess = cvpProcess;
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
        ingest(cvpBlobstoreHelper);
    }

    private void ingest(BlobstoreClientHelper blobstoreHelper) {
        resetCounters();
        LOGGER.info("Ingestion Started with BATCH PROCESSING LIMIT of {}", maxFilesToProcess);
        if (cvpProcess) {
            try {
                final Set<String> foldersSet = blobstoreHelper.getFolders();
                List<String> folders = foldersSet.stream().collect(Collectors.toList());
                Collections.shuffle(folders);

                LOGGER.debug("Folders found in CVP {} ", folders.size());
                folders.forEach(folder -> {

                    LOGGER.debug("--------------------------------------------");
                    LOGGER.debug("Inspecting folder: {}", folder);
                    final Set<SourceBlobItem> filteredSet = getFilesToIngest(folder, blobstoreHelper);
                    LOGGER.debug("filterSet size: {}", filteredSet.size());
                    filteredSet.forEach(file -> {
                        if (!batchProcessingLimitReached()) {
                            tallyItemsAttempted();
                            resolveMetaDataAndPostFileToHrs(file);
                        }
                    });
                    LOGGER.info("Running Total of Files Attempted: {}", itemsAttempted);
                });
            } catch (Exception ex) {
                LOGGER.error("CVP ingestion failed.", ex);
            }

        }
        if (vhProcess && maxFilesToProcess - itemsAttempted > 0) {
            try {
                var vhItems = vhBlobstoreClient.getItemsToProcess(Math.min(
                    (maxFilesToProcess - itemsAttempted),
                    vhProcessCount
                ));
                vhItems.forEach(sourceBlobItem -> {
                    tallyItemsAttempted();
                    LOGGER.info("ingesting : {}", sourceBlobItem.getFileUri());
                    resolveMetaDataAndPostFileToHrs(sourceBlobItem);
                });
                LOGGER.info("Filtered vhBlobItems count: {}", vhItems.size());
            } catch (Exception ex) {
                LOGGER.error("Vh processing failed ", ex);
            }
        }
        LOGGER.debug("Ingestion Complete");
        if (batchProcessingLimitReached()) {
            LOGGER.info("Batch Processing Limit Reached ({})", maxFilesToProcess);
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

    private void resolveMetaDataAndPostFileToHrs(SourceBlobItem sourceBlobItem) {
        try {
            LOGGER.debug("Resolving Filename {}", sourceBlobItem.getFilename());
            final Metadata metaData = metadataResolver.resolve(sourceBlobItem);
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
                sourceBlobItem.getFilename(),
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
                sourceBlobItem.getFilename(),
                e
            );
        }
    }

    private boolean batchProcessingLimitReached() {
        return itemsAttempted >= maxFilesToProcess;
    }

    private Set<SourceBlobItem> getFilesToIngest(final String folder, BlobstoreClientHelper blobstoreHelper) {
        try {
            LOGGER.debug("Getting CVP files in folder");
            final CvpItemSet cvpItemSet = blobstoreHelper.findByFolder(folder);
            LOGGER.debug("Getting HRS files already ingested");
            final HrsFileSet hrsFileSet = hrsApiClient.getIngestedFiles(folder);
            LOGGER.debug("Filtering out files not required from original cvp list");
            Set<SourceBlobItem> filesToIngest = ingestionFilterer.filter(cvpItemSet, hrsFileSet);

            int cvpFilesCount = cvpItemSet.getSourceBlobItems().size();
            int hrsFileCount = hrsFileSet.getHrsFiles().size();
            int filesToIngestCount = filesToIngest.size();

            tallyCvpFilesCountTotal(cvpFilesCount);
            tallyHrsFilesCountTotal(hrsFileCount);
            tallyFilesToIngestCount(filesToIngestCount);


            String ingestionStatus = determineFolderStatus(filesToIngestCount);

            LOGGER.debug("Folder:{}, CVP Files:{}, HRS Files:{}, To Ingest:{}, FOLDER-STATUS:{}",
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
