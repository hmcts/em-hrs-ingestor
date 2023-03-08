package uk.gov.hmcts.reform.em.hrs.ingestor.smoke;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource("classpath:application.yml")
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Hearing Recordings Ingestor";

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthEndpoint() {

        RestAssured.useRelaxedHTTPSValidation();

        String response =
            RestAssured
                .given()
                .baseUri(testUrl)
                .when()
                .get("/")
                .then()
                .statusCode(200).extract().body().asString();


        assertEquals(MESSAGE, response, "");
    }
}
