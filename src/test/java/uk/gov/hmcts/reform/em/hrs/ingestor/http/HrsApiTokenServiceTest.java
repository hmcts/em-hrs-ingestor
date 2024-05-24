package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrsApiTokenServiceTest {

    @Mock
    private IdamClient idamClient;

    private HrsApiTokenService hrsApiTokenService;

    private static final String USERNAME = "test-username";
    private static final String PASSWORD = "test-password";
    private static final String EXPECTED_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        hrsApiTokenService = new HrsApiTokenService(idamClient, USERNAME, PASSWORD);
    }

    @Test
    void testGetBearerToken() {
        when(idamClient.getAccessToken(USERNAME, PASSWORD)).thenReturn(EXPECTED_TOKEN);

        String actualToken = hrsApiTokenService.getBearerToken();

        assertEquals(EXPECTED_TOKEN, actualToken);
    }
}
