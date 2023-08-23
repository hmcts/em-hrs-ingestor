package uk.gov.hmcts.reform.em.hrs.ingestor.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsHttpClient;

@Configuration
public class AppConfig {

    @Value("${s2s.hrs.api.url}")
    private String hrsApiBaseUrl;

    @Bean
    public ObjectMapper provideObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        return objectMapper;
    }

    @Bean
    public Retrofit provideRetrofit(final ObjectMapper objectMapper, final OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
            .baseUrl(hrsApiBaseUrl)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .client(okHttpClient)
            .build();
    }

    @Bean
    public HrsHttpClient provideHrsHttpClient(final Retrofit retrofit) {
        return retrofit.create(HrsHttpClient.class);
    }

}
