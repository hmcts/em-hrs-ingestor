package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobstoreClientHelperImplTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlockBlobClient blockBlobClient;

    @Mock
    private PagedIterable<BlobItem> pagedIterable;

    private BlobstoreClientHelperImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new BlobstoreClientHelperImpl(blobContainerClient, 5);
    }

    @Test
    void getFolders_shouldReturnFilteredFolderNames() {
        // mock blob items
        BlobItem blobRecent = mockBlobItem("folder1/file1.txt", OffsetDateTime.now().minusDays(1));
        BlobItem blobOld = mockBlobItem("folder2/file2.txt", OffsetDateTime.now().minusDays(10));

        @SuppressWarnings("unchecked")
        PagedResponse<BlobItem> page = mock(PagedResponse.class);
        when(page.getValue()).thenReturn(List.of(blobRecent, blobOld));

        when(pagedIterable.streamByPage()).thenAnswer(invocation -> Stream.of(page));
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any())).thenReturn(pagedIterable);

        try (MockedStatic<BlobHelper> mocked = mockStatic(BlobHelper.class)) {
            mocked.when(() -> BlobHelper.parseFolderFromPath("folder1/file1.txt")).thenReturn("folder1");
            mocked.when(() -> BlobHelper.parseFolderFromPath("folder2/file2.txt")).thenReturn("folder2");

            // When
            Set<String> result = underTest.getFolders();

            // Then
            assertThat(result).containsExactly("folder1"); // folder2 excluded due to age
        }
    }

    @Test
    void findByFolder_shouldReturnCvpItemSet_withAndWithoutTrailingSlash() {
        // blob properties
        BlobItemProperties properties = mock(BlobItemProperties.class);
        when(properties.getContentMd5()).thenReturn("abc".getBytes());
        when(properties.getContentLength()).thenReturn(123L);

        BlobItem blobItem = new BlobItem().setName("folderX/file1.txt").setProperties(properties);

        @SuppressWarnings("unchecked")
        PagedResponse<BlobItem> page = mock(PagedResponse.class);
        when(page.getValue()).thenReturn(List.of(blobItem));

        when(pagedIterable.streamByPage()).thenAnswer(invocation -> Stream.of(page));
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any())).thenReturn(pagedIterable);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlockBlobClient()).thenReturn(blockBlobClient);
        when(blockBlobClient.getBlobUrl()).thenReturn("https://example.com/folderX/file1.txt");

        try (MockedStatic<BlobHelper> mocked = mockStatic(BlobHelper.class)) {
            mocked.when(() -> BlobHelper.getMd5Hash(any())).thenReturn("hash123");

            // folder without slash
            CvpItemSet setNoSlash = underTest.findByFolder("folderX");
            // folder with slash
            CvpItemSet setWithSlash = underTest.findByFolder("folderX/");

            assertThat(setNoSlash.getCvpFiles()).hasSize(1);
            assertThat(setWithSlash.getCvpFiles()).hasSize(1);

            SourceBlobItem item = setNoSlash.getSourceBlobItems().iterator().next();
            assertThat(item.getFilename()).isEqualTo("folderX/file1.txt");
            assertThat(item.getMd5Hash()).isEqualTo("hash123");
            assertThat(item.getContentLength()).isEqualTo(123L);
            assertThat(item.getHearingSource()).isEqualTo(HearingSource.CVP);
            assertThat(item.getFileUri()).isEqualTo("https://example.com/folderX/file1.txt");
        }
    }

    private BlobItem mockBlobItem(String name, OffsetDateTime creationTime) {
        BlobItemProperties props = mock(BlobItemProperties.class);
        when(props.getCreationTime()).thenReturn(creationTime);

        BlobItem blobItem = mock(BlobItem.class);
        when(blobItem.getName()).thenReturn(name);
        when(blobItem.getProperties()).thenReturn(props);
        return blobItem;
    }
}

