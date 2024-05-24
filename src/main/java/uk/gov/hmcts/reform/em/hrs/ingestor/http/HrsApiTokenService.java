package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.IdamClient;

@Service
@EnableFeignClients(basePackages = "uk.gov.hmcts.reform.idam")
@EnableAutoConfiguration
public class HrsApiTokenService {

    private IdamClient idamClient;
    private String hrsIngestorUserName;
    private String hrsIngestorUserPassword;

    @Autowired
    public HrsApiTokenService(
        IdamClient idamClient,
        @Value("${idam.hrs-ingestor.user-name}") String hrsIngestorUserName,
        @Value("${idam.hrs-ingestor.password}") String hrsIngestorUserPassword) {
        this.idamClient = idamClient;
        this.hrsIngestorUserName = hrsIngestorUserName;
        this.hrsIngestorUserPassword = hrsIngestorUserPassword;
    }

    public String getBearerToken() {
        return this.idamClient.getAccessToken(this.hrsIngestorUserName, this.hrsIngestorUserPassword);
    }
}
