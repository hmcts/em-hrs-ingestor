package uk.gov.hmcts.reform.em.hrs.ingestor.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public interface TestUtil {
    String TEST_FILE = "cf-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_0.txt";

    String TEST_FOLDER = "audiostream123";

    static byte[] getFileContent(final String filename) throws URISyntaxException, IOException {
        final URL resource = TestUtil.class.getClassLoader().getResource(filename);
        final File file = Paths.get(Objects.requireNonNull(resource).toURI()).toFile();
        return Files.readAllBytes(file.toPath());
    }

    static String convertObjectToJsonString(Object object) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        return objectMapper.writeValueAsString(object);
    }
}
