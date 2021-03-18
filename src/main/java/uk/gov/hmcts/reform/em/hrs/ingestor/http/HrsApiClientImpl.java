package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import retrofit2.Response;
import uk.gov.hmcts.reform.em.hrs.ingestor.domain.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.domain.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.HrsApiException;

import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HrsApiClientImpl implements HrsApiClient {
    private static final TypeReference<Set<String>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private final HrsHttpClient hrsHttpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public HrsApiClientImpl(final HrsHttpClient hrsHttpClient, final ObjectMapper objectMapper) {
        this.hrsHttpClient = hrsHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HrsFileSet getIngestedFiles(String folderName) throws HrsApiException, IOException {
        final Response<ResponseBody> response = hrsHttpClient.getFiles(folderName)
            .execute();

        if (response.isSuccessful()) {
            final Set<String> files = parseBody(response.body());
            return new HrsFileSet(files);
        } else {
            final String errorMessage = parseErrorBody(response.code(), response.message(), response.errorBody());
            throw new HrsApiException(errorMessage);
        }
    }

    @Override
    public void postFile(final String folder, final Metadata metadata) {
        // TODO
    }

    private Set<String> parseBody(final ResponseBody body) throws HrsApiException, IOException {
        if (body == null) {
            throw new HrsApiException("Response error: response body is null");
        } else {
            return objectMapper.readValue(body.string(), TYPE_REFERENCE);
        }
    }

    private String parseErrorBody(final int code,
                                  final String message,
                                  final ResponseBody body) throws HrsApiException, IOException {
        if (body == null) {
            throw new HrsApiException("Response error: " + code);
        } else {
            return "Response error: " + code + " => " + message + " => " + body.string();
        }
    }
}
