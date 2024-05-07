package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.parse.VhFileNameParser;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
public class VhBlobstoreClientHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(VhBlobstoreClientHelper.class);

    @Value("${azure.storage.vh-blob-operation-timeout-in-sec}")
    private int timeout;
    private final BlobContainerClient vhContainerClient;
    private final HearingSource hearingSource;
    private final BlobIndexHelper blobIndexHelper;

    private int processBackToDay;

    @Autowired
    public VhBlobstoreClientHelper(
        final @Qualifier("vhBlobContainerClient") BlobContainerClient blobContainerClient,
        @Value("${ingestion.vh.process-back-to-day}") int processBackToDay,
        BlobIndexHelper blobIndexHelper
    ) {
        this.vhContainerClient = blobContainerClient;
        this.processBackToDay = processBackToDay;
        this.blobIndexHelper = blobIndexHelper;
        this.hearingSource = HearingSource.VH;
    }

    public List<SourceBlobItem> getItemsToProcess(int limit) {
        final BlobListDetails blobListDetails = new BlobListDetails()
            .setRetrieveDeletedBlobs(false)
            .setRetrieveSnapshots(false);
        final ListBlobsOptions options = new ListBlobsOptions()
            .setDetails(blobListDetails);
        final Duration duration = Duration.ofMinutes(timeout);

        final PagedIterable<BlobItem> vhBlobItems = vhContainerClient.listBlobs(options, duration);

        var filteredBlobs = vhBlobItems
            .stream()
            .filter(blobItem -> VhFileNameParser.isValidFileName(blobItem.getName()))
            .filter(blobItem -> isNewFile(blobItem))
            .filter(blobItem ->
                        wrapFilterPredicate(
                            () -> vhContainerClient
                                .getBlobClient(blobItem.getName())
                                .getTags()
                                .getOrDefault("processed", "false")
                                .equalsIgnoreCase("false"))
            )
            .filter(blobItem -> wrapFilterPredicate(() -> blobIndexHelper.setIndexLease(blobItem.getName())))
            .map(blobItem -> transform(blobItem))
            .limit(limit)
            .toList();

        return filteredBlobs;

    }

    private boolean wrapFilterPredicate(Supplier<Boolean> sup) {
        try {
            return sup.get();
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isNewFile(BlobItem blobItem) {
        return OffsetDateTime.now().minusDays(processBackToDay).isBefore(blobItem.getProperties().getCreationTime());
    }

    public HearingSource getHearingSource() {
        return this.hearingSource;
    }

    private SourceBlobItem transform(final BlobItem blobItem) {

        final BlobItemProperties blobItemProperties = blobItem.getProperties();
        final String md5Hash = BlobHelper.getMd5Hash(blobItemProperties.getContentMd5());
        final String filename = blobItem.getName();

        return new SourceBlobItem(
            filename,
            getUrl(filename),
            md5Hash,
            blobItemProperties.getContentLength(),
            HearingSource.VH
        );

    }

    private String getUrl(final String filename) {
        final BlockBlobClient blobClient = vhContainerClient.getBlobClient(filename).getBlockBlobClient();
        return blobClient.getBlobUrl();
    }

}
