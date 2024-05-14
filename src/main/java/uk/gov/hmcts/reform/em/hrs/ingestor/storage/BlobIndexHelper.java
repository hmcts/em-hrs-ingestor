package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.options.BlobSetTagsOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.em.hrs.ingestor.util.DateUtil.EUROPE_LONDON_ZONE_ID;

@Component
public class BlobIndexHelper {

    private final BlobContainerClient blobContainerClient;
    private final int leaseForMinutes;
    @Value("${azure.storage.vh-blob-operation-timeout-in-sec}")
    private int timeout;

    public static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private static final Logger logger = getLogger(BlobIndexHelper.class);


    public BlobIndexHelper(
        final @Qualifier("vhBlobContainerClient") BlobContainerClient blobContainerClient,
        @Value("${azure.storage.vh-leased-for-minutes}") int leaseForMinutes) {
        this.blobContainerClient = blobContainerClient;
        this.leaseForMinutes = leaseForMinutes;
    }

    public boolean setIndexLease(String blobName) {
        try {
            var blobClient = blobContainerClient.getBlobClient(blobName);

            var tags = blobClient.getTags();
            var blobProperties = blobClient.getProperties();
            String etag = blobProperties.getETag();

            var zipFilename = blobClient.getBlobName();
            var containerName = blobClient.getContainerName();

            logger.info(
                "Checking if lease acquired on file {} in container {}. Tags: {}",
                zipFilename,
                containerName,
                tags
            );

            if (isLeaseExpired(tags)) {
                tags.put(
                    LEASE_EXPIRATION_TIME,
                    LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
                        .plusMinutes(leaseForMinutes).toString()
                );
                BlobSetTagsOptions options = new BlobSetTagsOptions(tags);

                options.setRequestConditions(new BlobRequestConditions().setIfMatch("\"" + etag + "\""));
                blobClient.setTagsWithResponse(
                    options,
                    Duration.ofSeconds(this.timeout),
                    Context.NONE
                );
                return true;
            } else {
                logger.info(
                    "Lease already acquired on file {} in container {}, existing tags: {}",
                    zipFilename,
                    containerName,
                    tags
                );
                return false;
            }
        } catch (Exception ex) {
            logger.info(
                "Could not get the lease {} ",
                blobName,
                ex
            );
            return false;
        }
    }

    private boolean isLeaseExpired(Map<String, String> tags) {

        String leaseExpirationTime = tags.get(LEASE_EXPIRATION_TIME);
        if (StringUtils.isBlank(leaseExpirationTime)) {
            return true; // lease not acquired on file
        } else {
            LocalDateTime leaseExpiresAt = LocalDateTime.parse(leaseExpirationTime);
            return leaseExpiresAt
                .isBefore(LocalDateTime.now(EUROPE_LONDON_ZONE_ID)); // check if lease expired
        }
    }
}
