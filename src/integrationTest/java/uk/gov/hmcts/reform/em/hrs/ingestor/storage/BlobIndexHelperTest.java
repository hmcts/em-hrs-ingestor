package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestAzureStorageConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.AzureOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    TestAzureStorageConfiguration.class,
    AzureOperations.class,
    BlobIndexHelper.class
})
public class BlobIndexHelperTest {

    @Autowired
    private AzureOperations azureOperations;
    @Autowired
    private BlobIndexHelper underTest;

    @BeforeEach
    void setup() {
        azureOperations.clearContainer();
    }

    @Test
    void should_lease_blob_if_available() {
        final String filePath = UUID.randomUUID() + ".txt";
        azureOperations.uploadToVhContainer(filePath, "Test data");

        var leaased = underTest.setIndexLease(filePath);

        assertThat(leaased).isTrue();
    }

    @Test
    void should_not_lease_blob_if_already_leased() {
        final String filePath = UUID.randomUUID() + ".txt";
        azureOperations.uploadToVhContainer(filePath, "Test data");
        var leaased = underTest.setIndexLease(filePath);
        assertThat(leaased).isEqualTo(true);
        var secondLeaased = underTest.setIndexLease(filePath);
        assertThat(secondLeaased).isFalse();
    }

    @Test
    void should_return_false_if_there_is_exception() {
        final String filePath = UUID.randomUUID() + ".txt";
        var leaased = underTest.setIndexLease(filePath);
        assertThat(leaased).isFalse();
    }
}
