package uk.gov.hmcts.reform.em.hrs.ingestor.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.BlobstoreClientHelper;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.BlobstoreClientHelperImpl;

@Configuration
public class AzureStorageConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageConfiguration.class);

    @Value("${azure.storage.cvp-storage-connection-string}")
    private String cvpConnectionString;

    @Value("${azure.storage.vh-storage-connection-string}")
    private String vhConnectionString;

    @Value("${azure.storage.cvp-storage-container-name}")
    private String cvpContainerName;

    @Value("${azure.storage.vh-storage-container-name}")
    private String vhContainerName;

    @Value("${azure.storage.use-ad-auth-for-source}")
    private boolean useAdForSourceBlobStorage;

    @Bean("cvpBlobstoreClientHelper")
    public BlobstoreClientHelper cvpBlobstoreClientHelper(
        @Qualifier("cvpBlobContainerClient") BlobContainerClient blobContainerClient,
        @Value("${ingestion.cvp.process-back-to-day}") int processBackToDay
    ) {
        LOGGER.info("creating CVP blob client ");
        return new BlobstoreClientHelperImpl(blobContainerClient, processBackToDay, HearingSource.CVP);
    }

    @Bean("cvpBlobContainerClient")
    public BlobContainerClient cvpBlobContainerClient() {
        LOGGER.info("creating CVP blob client");
        return getBlobClient(cvpConnectionString, cvpContainerName);
    }

    @Bean("vhBlobContainerClient")
    public BlobContainerClient vhBlobContainerClient() {
        LOGGER.info("creating VH blob client");
        return getBlobClient(vhConnectionString, vhContainerName);
    }

    private BlobContainerClient getBlobClient(String connectionString, String containerName) {
        LOGGER.info("connectionString : {}", StringUtils.left(connectionString, 60));
        LOGGER.info(
            "container name: {}, useAdForSourceBlobStorage:{}",
            containerName,
            useAdForSourceBlobStorage
        );

        //connectionstring is overloaded and used as endpoint when connecting to cvp, and connection string against
        // CFT/HRS test storage accounts
        if (useAdForSourceBlobStorage) {
            LOGGER.info("****************************");

            LOGGER.info(
                "Building client with default credential builder (will use SAS endpoint instead of attempt "
                    + "ManagedIdentityCredential");
            LOGGER.info("****************************");
            BlobContainerClientBuilder clientBuilder = new BlobContainerClientBuilder()
                .endpoint(connectionString)
                .containerName(containerName);

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            clientBuilder.credential(credential);
            return clientBuilder.buildClient();
        }

        BlobContainerClientBuilder clientBuilder = new BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName);


        BlobContainerClient blobContainerClient = clientBuilder.buildClient();

        final boolean containerExists = blobContainerClient.exists();

        if (!containerExists) {
            blobContainerClient.create();
        }

        return blobContainerClient;
    }

}
