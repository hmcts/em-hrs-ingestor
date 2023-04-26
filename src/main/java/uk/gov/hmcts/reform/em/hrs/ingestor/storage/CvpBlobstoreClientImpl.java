package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;

import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CvpBlobstoreClientImpl implements CvpBlobstoreClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CvpBlobstoreClient.class);

    private static final int BLOB_LIST_TIMEOUT = 30;
    private final BlobContainerClient blobContainerClient;

    @Value("${ingestion.process-back-to-day}")
    private int processBackToDay;

    @Autowired
    public CvpBlobstoreClientImpl(
        final @Qualifier("cvpBlobContainerClient") BlobContainerClient blobContainerClient
    ) {
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public Set<String> getFolders() {
        final BlobListDetails blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions options = new ListBlobsOptions()
            .setDetails(blobListDetails);
        final Duration duration = Duration.ofMinutes(BLOB_LIST_TIMEOUT);

        final PagedIterable<BlobItem> blobItems = blobContainerClient.listBlobs(options, duration);


        return blobItems.streamByPage()
            .flatMap(pagedResponse -> pagedResponse.getValue().stream()
                .filter(blobItem -> blobItem.getName().contains("/"))
                .filter(blobItem -> isNewFile(blobItem))
                .map(blobItem -> {
                    LOGGER.debug("Processing blobItem");
                    String filePath = blobItem.getName();
                    LOGGER.debug("File Path {}", filePath);
                    String folder = BlobHelper.parseFolderFromPath(filePath);
                    return folder;
                }))
            .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isNewFile(BlobItem blobItem) {
        return OffsetDateTime.now().minusDays(processBackToDay).isBefore(blobItem.getProperties().getCreationTime());
    }

    @Override
    public CvpItemSet findByFolder(final String folderName) {
        boolean folderNameIncludesTrailingSlash = StringUtils.endsWith(folderName, "/");

        final String folderPath = folderNameIncludesTrailingSlash ? folderName : folderName + File.separator;

        final BlobListDetails blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions options = new ListBlobsOptions()
            .setDetails(blobListDetails)
            .setPrefix(folderPath);
        final Duration duration = Duration.ofMinutes(BLOB_LIST_TIMEOUT);

        final PagedIterable<BlobItem> blobItems = blobContainerClient.listBlobs(options, duration);

        return transform(blobItems);

    }

    private CvpItemSet transform(final PagedIterable<BlobItem> blobItems) {
        final Set<CvpItem> files = blobItems.streamByPage()
            .flatMap(x -> x.getValue().stream().map(y -> {
                final BlobItemProperties blobItemProperties = y.getProperties();
                final String md5Hash = BlobHelper.getMd5Hash(blobItemProperties.getContentMd5());
                final String filename = y.getName();

                return new CvpItem(filename, getUrl(filename), md5Hash, blobItemProperties.getContentLength());
            }))
            .collect(Collectors.toUnmodifiableSet());

        return new CvpItemSet(files);
    }

    private String getUrl(final String filename) {
        final BlockBlobClient blobClient = blobContainerClient.getBlobClient(filename).getBlockBlobClient();
        return blobClient.getBlobUrl();
    }


}
