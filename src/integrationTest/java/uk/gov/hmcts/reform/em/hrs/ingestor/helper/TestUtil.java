package uk.gov.hmcts.reform.em.hrs.ingestor.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public interface TestUtil {
    String TEST_FILE = "cf-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_0.txt";

    String TEST_FILE_VH = "HRS-0123-FUNCTEST_2022-03-24---08-03-10---390_2020-11-04-14.56.32.819-UTC_0.mp4";
    String TEST_FOLDER = "audiostream123";

    static byte[] getFileContent(final String filename) throws URISyntaxException, IOException {
        final URL resource = TestUtil.class.getClassLoader().getResource(filename);
        final File file = Paths.get(Objects.requireNonNull(resource).toURI()).toFile();
        return Files.readAllBytes(file.toPath());
    }

    static String convertObjectToJsonString(Object object) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        return objectMapper.writeValueAsString(object);
    }
}
