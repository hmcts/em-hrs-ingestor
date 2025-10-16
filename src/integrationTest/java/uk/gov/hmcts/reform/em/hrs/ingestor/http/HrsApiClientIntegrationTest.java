package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.AppConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestOkHttpClientConfig;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.HrsApiException;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.mock.WireMockInitializer;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.IdamCacheExpiry;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.IdamCachedClient;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.io.IOException;
import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.em.hrs.ingestor.helper.TestUtil.convertObjectToJsonString;

@SpringBootTest(classes = {TestOkHttpClientConfig.class, AppConfig.class, HrsApiClientImpl.class,
    HrsApiTokenService.class, IdamCachedClient.class, IdamCacheExpiry.class, IdamClient.class, IdamApi.class})
@ContextConfiguration(initializers = {WireMockInitializer.class})
class HrsApiClientIntegrationTest {
    private static final String TEST_FILE = "file.mp4";
    private static final String TEST_FOLDER = "folder-1";
    private static final String GET_PATH = String.format("/folders/%s", TEST_FOLDER);
    private static final String POST_PATH = "/segments";
    private static final Metadata METADATA = new Metadata(
        TEST_FOLDER,
        "recording-file-name",
        "recording-cvp-uri",
        1L,
        "I2foA30B==",
        "recording-ref",
        0,
        "mp4",
        LocalDateTime.now(),
        "xyz",
        HearingSource.CVP,
        222,
        "AB",
        "C3",
        "AAA1",
        "interpreter"
    );
    private static final String TEST_ACCESS = "Bearer test-access";
    private static final String AUTHORIZATION = "Authorization";
    private static final String CONTENT_TYPE = "Content-Type";

    private WireMockServer wireMockServer;
    private HrsApiClientImpl underTest;

    @Autowired
    public HrsApiClientIntegrationTest(
        WireMockServer wireMockServer,
        HrsApiClientImpl underTest
    ) {
        this.wireMockServer = wireMockServer;
        this.underTest = underTest;
    }

    @BeforeEach
    public void prepare() {
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
                .withHeader(AUTHORIZATION, equalTo(TEST_ACCESS))
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
    }


    @Test
    void testShouldGetDataSuccessfully() throws IOException, HrsApiException {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .withHeader(AUTHORIZATION, equalTo(TEST_ACCESS))
                .willReturn(aResponse()
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody("{\"folder-name\":\"" + TEST_FOLDER + "\",\"filenames\":[\"file.mp4\"]}"))
        );
        final HrsFileSet ingestedFiles = underTest.getIngestedFiles(TEST_FOLDER);

        assertThat(ingestedFiles.getHrsFiles()).singleElement().isEqualTo(TEST_FILE);
        wireMockServer.verify(exactly(1), getRequestedFor(urlEqualTo(String.format(GET_PATH, TEST_FOLDER)))
            .withHeader(AUTHORIZATION, equalTo(TEST_ACCESS)));

    }

    @Test
    void testShouldThrowHrsApiExceptionWhenNonSuccessStatusCodeIsReceived() {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .willReturn(aResponse()
                                .withStatus(403))
        );
        assertThatExceptionOfType(HrsApiException.class).isThrownBy(() -> underTest.getIngestedFiles(TEST_FOLDER));
        wireMockServer.verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo(String.format(GET_PATH, TEST_FOLDER))));
    }

    @Test
    void testShouldRaiseExceptionWhenResponseTimesOut() {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .willReturn(aResponse()
                                .withFixedDelay(1000)
                                .withStatus(200))
        );

        assertThatIOException().isThrownBy(() -> underTest.getIngestedFiles(TEST_FOLDER));
    }

    @Test
    void testShouldRaiseExceptionWhenResponseBodyIsEmpty() {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withFault(Fault.EMPTY_RESPONSE))
        );

        assertThatIOException().isThrownBy(() -> underTest.getIngestedFiles(TEST_FOLDER));
    }

    @Test
    void testShouldRaiseExceptionWhenResponseHasFaultyBody() {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .willReturn(aResponse()
                                .withChunkedDribbleDelay(20, 1000)
                                .withStatus(200))
        );

        assertThatIOException().isThrownBy(() -> underTest.getIngestedFiles(TEST_FOLDER));
    }

    @Test
    void testShouldRaiseExceptionWhenConnectionResets() {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .willReturn(aResponse()
                                .withFault(Fault.CONNECTION_RESET_BY_PEER))
        );

        assertThatIOException().isThrownBy(() -> underTest.getIngestedFiles(TEST_FOLDER));
    }

    @Test
    void testShouldRaiseExceptionWhenTimeoutGettingErrorBody() {
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo(GET_PATH))
                .willReturn(aResponse()
                                .withFixedDelay(1000)
                                .withStatus(403))
        );

        assertThatIOException().isThrownBy(() -> underTest.getIngestedFiles(TEST_FOLDER));
    }

    @Test
    void testShouldPostDataSuccessfully() throws IOException, HrsApiException {
        final String expectedPayload = convertObjectToJsonString(METADATA);
        wireMockServer.stubFor(
            WireMock.post(urlPathEqualTo(POST_PATH))
                .willReturn(aResponse()
                                .withStatus(202))
        );

        underTest.postFile(METADATA);

        wireMockServer.verify(exactly(1), postRequestedFor(urlEqualTo(POST_PATH))
            .withRequestBody(equalTo(expectedPayload)));
    }

    @Test
    void testShouldThrowHrsApiExceptionWhenPostReceivesNonSuccessStatusCode() {
        wireMockServer.stubFor(
            WireMock.post(urlPathEqualTo(POST_PATH))
                .willReturn(aResponse()
                                .withStatus(404))
        );

        assertThatExceptionOfType(HrsApiException.class).isThrownBy(() -> underTest.postFile(METADATA));
        wireMockServer.verify(exactly(1), postRequestedFor(urlEqualTo(POST_PATH)));
    }

    @Test
    void testShouldRaiseExceptionWhenPostTimesOut() {
        wireMockServer.stubFor(
            WireMock.post(urlPathEqualTo(POST_PATH))
                .willReturn(aResponse()
                                .withFixedDelay(1000)
                                .withStatus(202))
        );

        assertThatIOException().isThrownBy(() -> underTest.postFile(METADATA));
    }

    @Test
    void testShouldRaiseExceptionWhenConnectionResetsOnPost() {
        wireMockServer.stubFor(
            WireMock.post(urlPathEqualTo(POST_PATH))
                .willReturn(aResponse()
                                .withFault(Fault.CONNECTION_RESET_BY_PEER))
        );

        assertThatIOException().isThrownBy(() -> underTest.postFile(METADATA));
    }

    @Test
    void testShouldRaiseExceptionWhenTimeoutGettingErrorBodyOnPost() {
        wireMockServer.stubFor(
            WireMock.post(urlPathEqualTo(POST_PATH))
                .willReturn(aResponse()
                                .withFixedDelay(1000)
                                .withStatus(403))
        );

        assertThatIOException().isThrownBy(() -> underTest.postFile(METADATA));
    }

}
