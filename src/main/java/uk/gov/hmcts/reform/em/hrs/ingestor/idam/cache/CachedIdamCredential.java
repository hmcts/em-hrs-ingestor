package uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache;

/**
 * Represents a cached IDAM credential.
 */
public class CachedIdamCredential {

    public final String accessToken;
    public final String userId;
    public final long expiresIn;

    /**
     * Constructor for the CachedIdamCredential.
     * @param accessToken the access token
     * @param userId the user ID
     * @param expiresIn the time in seconds until the token expires
     */
    public CachedIdamCredential(String accessToken, String userId, long expiresIn) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.expiresIn = expiresIn;
    }
}
