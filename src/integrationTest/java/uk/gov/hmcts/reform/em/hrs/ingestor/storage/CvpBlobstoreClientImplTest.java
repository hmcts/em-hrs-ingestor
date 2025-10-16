package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.ingestor.config.TestAzureStorageConfiguration;
import uk.gov.hmcts.reform.em.hrs.ingestor.helper.AzureOperations;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    TestAzureStorageConfiguration.class,
    AzureOperations.class
})
class CvpBlobstoreClientImplTest {
    private static final String FOLDER_ONE = "folder-1";
    private static final String FOLDER_TWO = "folder-2";
    private static final String FOLDER_THREE = "folder-3";
    private static final String EMPTY_FOLDER = "folder-0";
    private static final String ONE_ITEM_FOLDER = "one-item-folder";
    private static final String MANY_ITEMS_FOLDER = "many-items-folder";
    private static final String TEST_DATA = "Hello World!";
    private final Random random = new SecureRandom();

    private AzureOperations azureOperations;
    private BlobstoreClientHelperImpl underTest;

    @Autowired
    public CvpBlobstoreClientImplTest(
        AzureOperations azureOperations,
        BlobstoreClientHelperImpl underTest
    ) {
        this.azureOperations = azureOperations;
        this.underTest = underTest;
    }

    @BeforeEach
    void setup() {
        azureOperations.clearContainer();
    }

    @Test
    void testShouldGetFolders() {
        populateCvpBlobstore();

        final Set<String> folders = underTest.getFolders();

        assertThat(folders).isNotEmpty().contains(FOLDER_ONE, FOLDER_TWO, FOLDER_THREE);
    }

    @Test
    void testShouldReturnEmptySetWhenFolderDoesNotExist() {
        final CvpItemSet cvpItemSet = underTest.findByFolder(EMPTY_FOLDER);

        assertThat(cvpItemSet.getSourceBlobItems()).isEmpty();
    }

    @Test
    void testShouldReturnASetContainingOneWhenFolderContainsOneItem() throws NoSuchAlgorithmException {
        final String filePath = getFolderPath(ONE_ITEM_FOLDER) + UUID.randomUUID() + ".txt";
        final String expectedHash = generateMd5Hash(TEST_DATA);
        azureOperations.uploadToContainer(filePath, TEST_DATA);

        final CvpItemSet cvpItemSet = underTest.findByFolder(ONE_ITEM_FOLDER);

        assertThat(cvpItemSet.getSourceBlobItems()).singleElement().satisfies(x -> {
            assertThat(x.getFilename()).isEqualTo(filePath);
            assertThat(x.getMd5Hash()).isEqualTo(expectedHash);
            assertThat(x.getFileUri())
                .startsWith("http://localhost:")
                .contains("/devstoreaccount1/cvp-test-container/one-item-folder");
        });
    }

    @Test
    void testShouldReturnSetContainingMultipleFilenamesWhenFolderContainsMultipleItems() {
        final Set<String> filePaths = generateFilePaths();
        azureOperations.uploadToContainer(filePaths);

        final CvpItemSet cvpItemSet = underTest.findByFolder(MANY_ITEMS_FOLDER);

        assertThat(cvpItemSet.getCvpFiles()).hasSameElementsAs(filePaths);
    }


    @Test
    void testShouldReturnContentsOfFolderNotWildcardForFoldername() {
        populateCvpBlobstoreWithSimilarFoldernames();

        final CvpItemSet cvpItemSet = underTest.findByFolder("/multi1");

        assertThat(cvpItemSet.getCvpFiles().stream().count()).isEqualTo(1);
    }


    private void populateCvpBlobstore() {
        final Set<String> filePaths = Set.of(
            getFolderPath(FOLDER_ONE) + UUID.randomUUID() + ".txt",
            getFolderPath(FOLDER_TWO) + UUID.randomUUID() + ".txt",
            getFolderPath(FOLDER_THREE) + UUID.randomUUID() + ".txt"
        );
        azureOperations.uploadToContainer(filePaths);
    }

    private void populateCvpBlobstoreWithSimilarFoldernames() {
        final Set<String> filePaths = Set.of(
            "/multi1/" + UUID.randomUUID().toString() + ".txt",
            "/multi10/" + UUID.randomUUID().toString() + ".txt",
            "/multi1000/" + UUID.randomUUID().toString() + ".txt"
        );
        azureOperations.uploadToContainer(filePaths);
    }

    private Set<String> generateFilePaths() {

        final int number = random.nextInt(8) + 2;

        return IntStream.rangeClosed(1, number)
            .mapToObj(x -> MANY_ITEMS_FOLDER + "/f" + x + ".txt")
            .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("java:S4790") // Safe: only used in tests
    private String generateMd5Hash(String testData) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(testData.getBytes());
        final byte[] digest = md.digest();
        return Base64.getEncoder().encodeToString(digest);
    }

    private String getFolderPath(String folderName) {
        return folderName + "/";
    }
}
