package uk.gov.hmcts.reform.em.hrs.ingestor.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureStorageConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageConfiguration.class);


    @Value("${azure.storage.cvp-storage-connection-string}")
    private String connectionString;

    @Value("${azure.storage.cvp-storage-container-name}")
    private String cvpContainerName;

    @Value("${azure.storage.use-ad-auth-for-source}")
    private boolean useAdForSourceBlobStorage;

    @Bean
    BlobContainerClient provideBlobContainerClient() {
        LOGGER.info("""
        ****************************
                 Starting Up
        ****************************""");
        LOGGER.info("cvp connection string(60): {}", StringUtils.left(connectionString, 60));
        LOGGER.info("cvp container name: {}, useAdForSourceBlobStorage:{}", cvpContainerName, useAdForSourceBlobStorage);

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
                .containerName(cvpContainerName);

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            clientBuilder.credential(credential);
            return clientBuilder.buildClient();
        }

        BlobContainerClientBuilder clientBuilder = new BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(cvpContainerName);


        BlobContainerClient blobContainerClient = clientBuilder.buildClient();

        final boolean containerExists = blobContainerClient.exists();

        if (!containerExists) {
            blobContainerClient.create();
        }

        return blobContainerClient;
    }
}
