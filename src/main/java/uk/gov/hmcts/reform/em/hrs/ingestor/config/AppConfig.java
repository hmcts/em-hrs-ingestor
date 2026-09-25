package uk.gov.hmcts.reform.em.hrs.ingestor.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsHttpClient;

@Configuration
public class AppConfig {

    public static final String HRS_API_OBJECT_MAPPER = "hrsApiObjectMapper";

    @Value("${s2s.hrs.api.url}")
    private String hrsApiBaseUrl;

    /**
     * Dedicated Jackson 2 mapper for Retrofit HRS API calls (kebab-case, NON_NULL).
     * Boot 4 defaults to Jackson 3 for Feign/MVC; retrofit converter-jackson 3.0.0 still requires
     * {@code com.fasterxml.jackson}. Qualifier keeps this bean off Feign/MVC converters.
     * Remove when Retrofit ships a Jackson 3 converter.
     */
    @Bean(name = HRS_API_OBJECT_MAPPER)
    public ObjectMapper hrsApiObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        return objectMapper;
    }

    @Bean
    public Retrofit provideRetrofit(
        @Qualifier(HRS_API_OBJECT_MAPPER) final ObjectMapper hrsApiObjectMapper,
        final OkHttpClient okHttpClient
    ) {
        return new Retrofit.Builder()
            .baseUrl(hrsApiBaseUrl)
            .addConverterFactory(JacksonConverterFactory.create(hrsApiObjectMapper))
            .client(okHttpClient)
            .build();
    }

    @Bean
    public HrsHttpClient provideHrsHttpClient(final Retrofit retrofit) {
        return retrofit.create(HrsHttpClient.class);
    }

}
