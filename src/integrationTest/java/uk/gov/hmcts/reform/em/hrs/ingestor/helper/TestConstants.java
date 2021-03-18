package uk.gov.hmcts.reform.em.hrs.ingestor.helper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public interface TestConstants {
    String INFECTED_FILE = "eicar-standard-av-test-file";
    String CLEAN_FILE = "clean-file";
    String INFECTED_FOLDER = "infected-folder";
    String CLEAN_FOLDER = "clean-folder";

    static byte[] getFileContent(final String filename) throws URISyntaxException, IOException {
        final URL resource = TestConstants.class.getClassLoader().getResource(filename);
        final File file = Paths.get(Objects.requireNonNull(resource).toURI()).toFile();

        return Files.readAllBytes(file.toPath());
    }
}
