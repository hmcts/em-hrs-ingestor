package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.HrsApiException;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Component
public class HrsApiClientImpl implements HrsApiClient {
    private static final TypeReference<RecordingFilenameDto> TYPE_REFERENCE = new TypeReference<>() {
    };

    private final HrsHttpClient hrsHttpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public HrsApiClientImpl(final HrsHttpClient hrsHttpClient, final ObjectMapper objectMapper) {
        this.hrsHttpClient = hrsHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HrsFileSet getIngestedFiles(String folderName) throws IOException, HrsApiException {
        final Response<ResponseBody> response = hrsHttpClient.getFiles(folderName)
            .execute();

        if (!response.isSuccessful()) {
            throw new HrsApiException(
                response.code(),
                response.message(),
                Objects.requireNonNull(response.errorBody())
            );
        }

        final RecordingFilenameDto body = parseBody(response.body());
        final Set<String> files = body.getFilenames();

        return new HrsFileSet(files);
    }

    @Override
    public void postFile(final Metadata metadata) throws IOException, HrsApiException {
        final Response<ResponseBody> response = hrsHttpClient.postFile(metadata).execute();
        boolean isSuccessful = response.isSuccessful();
        if (!isSuccessful) {
            throw new HrsApiException(
                response.code(),
                response.message(),
                Objects.requireNonNull(response.errorBody())
            );
        }

    }

    private RecordingFilenameDto parseBody(final ResponseBody body) throws IOException {
        return objectMapper.readValue(Objects.requireNonNull(body).string(), TYPE_REFERENCE);
    }


}
