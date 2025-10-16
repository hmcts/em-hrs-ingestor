package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.AppConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestAzureStorageConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestOkHttpClientConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.AzureOperations;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestUtil;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsApiClientImpl;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsApiTokenService;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.mock.WireMockInitializer;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.IdamCacheExpiry;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.IdamCachedClient;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestUtil.TEST_FILE;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestUtil.TEST_FOLDER;

@SpringBootTest(classes = {
    TestOkHttpClientConfig.class,
    TestAzureStorageConfiguration.class,
    AppConfig.class,
    HrsApiClientImpl.class,
    IngestionFiltererImpl.class,
    MetadataResolverImpl.class,
    DefaultIngestorService.class,
    AzureOperations.class,
    HrsApiTokenService.class,
    IdamCachedClient.class,
    IdamCacheExpiry.class
})
@ContextConfiguration(initializers = {WireMockInitializer.class})
class IngestorServiceIntegrationTest {
    private static final String GET_FOLDERS_PATH = "/folders/([a-zA-Z0-9_.-]*)";
    private static final String POST_PATH = "/segments";
    private static final String DUMMY_FOLDER = "dummy-folder";
    private static final String CONTENT_TYPE = "Content-Type";
    private WireMockServer wireMockServer;
    private AzureOperations azureOperations;
    private DefaultIngestorService underTest;

    @Autowired
    public IngestorServiceIntegrationTest(
        WireMockServer wireMockServer,
        AzureOperations azureOperations,
        DefaultIngestorService underTest
    ) {
        this.wireMockServer = wireMockServer;
        this.azureOperations = azureOperations;
        this.underTest = underTest;
    }

    @BeforeEach
    public void prepare() {
        azureOperations.clearContainer();
        wireMockServer.resetAll();
        wireMockServer.stubFor(
            WireMock.post(urlPathEqualTo("/o/token"))
                .willReturn(aResponse()
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody("""
                                              {
                                                "access_token": "test-access",
                                                "expires_in": "3600",
                                                "id_token": "test-id",
                                                "refresh_token": "test-refresh",
                                                "scope": "openid profile email",
                                                "token_type": "Bearer"
                                              }
                                              """))
        );

        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo("/o/userinfo"))
                .withHeader("Authorization", equalTo("Bearer test-access"))
                .willReturn(aResponse()
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withStatus(200)
                                .withBody("""
                                              {
                                                "sub": "1234567890",
                                                "name": "John Doe",
                                                "email": "johndoe@example.com",
                                                "preferred_username": "johndoe",
                                                "given_name": "John",
                                                "family_name": "Doe"
                                              }
                                              """))
        );
        wireMockServer.stubFor(
            get(urlMatching(GET_FOLDERS_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withBody("{\"folder-name\":\"" + DUMMY_FOLDER + "\",\"filenames\":[]}"))
        );

        wireMockServer.stubFor(
            post(urlMatching(POST_PATH))
                .willReturn(aResponse()
                    .withStatus(202))
        );
    }

    @Test
    void testShouldIngestFiles() throws URISyntaxException, IOException {
        setupCvpBlobstore();
        underTest.ingest();

        wireMockServer.verify(
            exactly(1),
            postRequestedFor(urlEqualTo(POST_PATH))
        );
    }

    private void setupCvpBlobstore() throws URISyntaxException, IOException {
        final byte[] data = TestUtil.getFileContent(TEST_FILE);
        azureOperations.uploadToContainer(TEST_FOLDER + "/" + TEST_FILE, data);
    }

}
