package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.hrs.ingestor.idam.cache.IdamCachedClient;

@Service
@EnableFeignClients(basePackages = "uk.gov.hmcts.reform.idam")
@EnableAutoConfiguration
public class HrsApiTokenService {

    private IdamCachedClient idamCachedClient;

    @Autowired
    public HrsApiTokenService(IdamCachedClient idamClient) {
        this.idamCachedClient = idamClient;
    }

    public String getBearerToken() {
        return this.idamCachedClient.getIdamCredentials().accessToken;
    }
}
