package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.av.AntivirusClientImpl;
import uk.gov.hmcts.reform.em.hrs.ingestor.av.mock.ClamAvInitializer;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.AppConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.ClamAvConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestAzureStorageConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestOkHttpClientConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.AzureOperations;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestConstants;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsApiClientImpl;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.mock.WireMockInitializer;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.CvpBlobstoreClientImpl;

import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestConstants.CLEAN_FILE;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestConstants.CLEAN_FOLDER;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestConstants.INFECTED_FILE;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestConstants.INFECTED_FOLDER;

@SpringBootTest(classes = {
    TestOkHttpClientConfig.class,
    TestAzureStorageConfiguration.class,
    ClamAvConfig.class,
    AppConfig.class,
    CvpBlobstoreClientImpl.class,
    HrsApiClientImpl.class,
    AntivirusClientImpl.class,
    IngestionFiltererImpl.class,
    MetadataResolverImpl.class,
    DefaultIngestorService.class,
    AzureOperations.class
})
@ContextConfiguration(initializers = {WireMockInitializer.class, ClamAvInitializer.class})
class IngestorServiceIntegrationTest {
    private static final String GET_PATH = "/folders/([a-zA-Z0-9_.-]*)/hearing-recording-file-names";
    private static final String POST_PATH = "/folders/([a-zA-Z0-9_.-]*)/hearing-recording";
    private static final String EXPECTED_PATH = "/folders/%s/hearing-recording";

    @Inject
    private WireMockServer wireMockServer;
    @Inject
    private AzureOperations azureOperations;
    @Inject
    private DefaultIngestorService underTest;

    @BeforeEach
    public void prepare() {
        azureOperations.clearContainer();
        wireMockServer.resetAll();

        wireMockServer.stubFor(
            get(urlMatching(GET_PATH))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                                .withBody("[]"))
        );

        wireMockServer.stubFor(
            post(urlMatching(POST_PATH))
                .willReturn(aResponse()
                                .withStatus(202))
        );
    }

    @Test
    @Disabled
    void testShouldIngestCleanFiles() throws Exception {
        setupCvpBlobstore(CLEAN_FOLDER, CLEAN_FILE);

        underTest.ingest();

        wireMockServer.verify(
            exactly(1),
            postRequestedFor(urlEqualTo(String.format(EXPECTED_PATH, CLEAN_FOLDER)))
        );
    }

    @Test
    void testShouldNotIngestInfectedFiles() throws Exception {
        setupCvpBlobstore(INFECTED_FOLDER, INFECTED_FILE);

        underTest.ingest();

        wireMockServer.verify(
            exactly(0),
            postRequestedFor(urlEqualTo(String.format(EXPECTED_PATH, INFECTED_FOLDER)))
        );
    }

    private void setupCvpBlobstore(final String folder, final String file) throws Exception {
        final byte[] data = TestConstants.getFileContent(file);
        azureOperations.uploadToContainer(folder + "/" + file, data);
    }

}
