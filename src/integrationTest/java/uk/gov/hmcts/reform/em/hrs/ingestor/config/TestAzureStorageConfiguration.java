package uk.gov.hmcts.reform.em.hrs.ingestor.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
public class TestAzureStorageConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAzureStorageConfiguration.class);

    private static final String AZURITE_IMAGE = "mcr.microsoft.com/azure-storage/azurite";
    private static final int MAPPER_PORT = 10000;

    private static final String ACCOUNT_NAME = "devstoreaccount1";
    private static final String ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final String BLOB_ENDPOINT = "http://%s:%d/%s";
    private static final String AZURITE_CREDENTIALS =
        "DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=%s;";
    private static final String CONTAINER_NAME = "hrs-test-container";

    //docker run -p 10000:10000 mcr.microsoft.com/azure-storage/azurite azurite-blob --blobHost 0.0.0.0 --blobPort 10000
    private final GenericContainer<?> azuriteContainer = new GenericContainer<>(AZURITE_IMAGE)
        .withExposedPorts(MAPPER_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
        .waitingFor(Wait.forListeningPort())
        .withCommand("azurite-blob --blobHost 0.0.0.0 --blobPort 10000");

    @PostConstruct
    void init() {
        if (!azuriteContainer.isRunning()) {
            azuriteContainer.start();
        }
    }

    @Bean
    @Primary
    public BlobContainerClient provideBlobContainerClient() {
        final String blobServiceUrl = String.format(
            BLOB_ENDPOINT,
            azuriteContainer.getHost(),
            azuriteContainer.getMappedPort(MAPPER_PORT),
            ACCOUNT_NAME
        );
        final String connectionString = String.format(AZURITE_CREDENTIALS, ACCOUNT_NAME, ACCOUNT_KEY, blobServiceUrl);

        final BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(CONTAINER_NAME)
            .buildClient();

        blobContainerClient.create();

        return blobContainerClient;
    }

    @PreDestroy
    void cleanUp() {
        if (azuriteContainer.isRunning()) {
            azuriteContainer.stop();
        }
    }
}
