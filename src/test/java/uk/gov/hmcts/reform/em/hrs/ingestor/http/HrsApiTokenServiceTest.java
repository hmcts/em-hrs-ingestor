package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.IdamCachedClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrsApiTokenServiceTest {

    @Mock
    private IdamCachedClient idamClient;

    private HrsApiTokenService hrsApiTokenService;

    private static final String EXPECTED_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        hrsApiTokenService = new HrsApiTokenService(idamClient);
    }

    @Test
    void testGetBearerToken() {
        CachedIdamCredential cachedIdamCredential = new CachedIdamCredential(EXPECTED_TOKEN, "test-user-id", 28800);
        when(idamClient.getIdamCredentials()).thenReturn(cachedIdamCredential);
        when(idamClient.getIdamCredentials()).thenReturn(cachedIdamCredential);

        String actualToken = hrsApiTokenService.getBearerToken();

        assertEquals(EXPECTED_TOKEN, actualToken);
    }
}
