package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestAzureStorageConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.AzureOperations;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.em.hrs.ingestor.util.DateUtil.EUROPE_LONDON_ZONE_ID;

@SpringBootTest(classes = {
    TestAzureStorageConfiguration.class,
    AzureOperations.class,
    VhBlobstoreClientHelper.class,
    BlobIndexHelper.class
})
public class VhBlobstoreClientHelperTest {

    @Autowired
    private AzureOperations azureOperations;
    @Autowired
    private VhBlobstoreClientHelper underTest;

    @Autowired
    @Qualifier("vhBlobContainerClient")
    private BlobContainerClient vhBlobContainerClient;

    @BeforeEach
    void setup() {
        azureOperations.clearContainer();
    }

    @Test
    void should_return_blob_if_available() {
        final String filePath = UUID.randomUUID() + ".mp4";
        azureOperations.uploadToVhContainer(filePath, "Test data");

        var itemsToProcess = underTest.getItemsToProcess();

        assertThat(itemsToProcess).isNotEmpty();
    }

    @Test
    void should_not_return_blob_if_not_had_mp() {
        // will be filtered because of ".txt"
        final String filePath1 = UUID.randomUUID() + ".txt";
        azureOperations.uploadToVhContainer(filePath1, "Test data");
        // will be filtered because already  leased
        final String filePath2 = UUID.randomUUID() + ".mp";
        azureOperations.uploadToVhContainer(filePath2, "Test data");
        vhBlobContainerClient
            .getBlobClient(filePath2)
            .setTags(
                Map.of(
                    "leaseExpirationTime",
                    LocalDateTime.now(EUROPE_LONDON_ZONE_ID).plusMinutes(100).toString()
                )
            );
        // will get
        final String filePath3 = UUID.randomUUID() + ".mp3";
        azureOperations.uploadToVhContainer(filePath3, "Test data");

        // will be filtered already processed
        final String filePath4 = UUID.randomUUID() + ".mp4";
        azureOperations.uploadToVhContainer(filePath4, "Test data");
        vhBlobContainerClient
            .getBlobClient(filePath4)
            .setTags(Map.of("processed", "trUe"));

        var itemsToProcess = underTest.getItemsToProcess();

        assertThat(itemsToProcess).hasSize(1);
        var item = itemsToProcess.get(0);
        assertThat(item.getFilename()).isEqualTo(filePath3);
        assertThat(item.getFileUri()).endsWith(filePath3);
        assertThat(item.getContentLength()).isEqualTo(9);
        assertThat(item.getMd5Hash()).isEqualTo("yh6gLBC3w39CW5t92G1eEQ==");
        assertThat(item.getHearingSource()).isEqualTo(HearingSource.VH);

    }
}
