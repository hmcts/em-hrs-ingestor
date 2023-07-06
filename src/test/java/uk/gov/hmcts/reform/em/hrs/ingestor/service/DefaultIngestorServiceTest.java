package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.HrsApiException;
import uk.gov.hmcts.reform.em.hrs.ingestor.http.HrsApiClient;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.storage.BlobstoreClientHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultIngestorServiceTest {
    private static final String FOLDER_ONE = "folder-1";
    private static final String FOLDER_TWO = "folder-2";
    private static final String FOLDER_THREE = "folder-3";
    private static final CvpItem CVP_FILE_1 = new CvpItem("f1.mp4", "uri1", "hash1", 1L);
    private static final CvpItem CVP_FILE_2 = new CvpItem("f2.mp4", "uri2", "hash2", 1L);
    private static final CvpItem CVP_FILE_3 = new CvpItem("f3.mp4", "uri3", "hash3", 1L);
    private static final Set<CvpItem> CVP_FILES_1_2_3_AS_SET = Set.of(CVP_FILE_1,CVP_FILE_2,CVP_FILE_3);

    private static final CvpItemSet CVP_ITEMSET_OF_3_FILES;

    static {
        CVP_ITEMSET_OF_3_FILES = new CvpItemSet(Set.of(
            CVP_FILE_1,
            CVP_FILE_2,
            CVP_FILE_3
        ));
    }

    private static final HrsFileSet HRS_FILESET_OF_2_FILES = new HrsFileSet(Set.of("f1.mp4", "f2.mp4"));
    private static final HrsFileSet HRS_FILESET_OF_0_FILES = new HrsFileSet(Collections.emptySet());

    private static final Metadata METADATA = new Metadata(
        "audiostream222",
        "recording-file-name",
        "recording-cvp-uri",
        "recording-cvp-uri",
        1L,
        "I2foA30B==",
        null,
        0,
        "mp4",
        LocalDateTime.now(),
        "xyz",
        HearingSource.CVP,
        222,
        "AB",
        null,
        null
    );
    @Mock
    private BlobstoreClientHelper cvpBlobstoreClient;
    @Mock
    private HrsApiClient hrsApiClient;
    @Mock
    private IngestionFilterer ingestionFilterer;
    @Mock
    private MetadataResolver metadataResolver;


    @InjectMocks
    private DefaultIngestorService underTest;

    @Test
    void testShouldIngestOneFolder() throws Exception {
        doReturn(Set.of(FOLDER_ONE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(FOLDER_ONE);
        doReturn(HRS_FILESET_OF_2_FILES).when(hrsApiClient).getIngestedFiles(FOLDER_ONE);
        doReturn(Set.of(CVP_FILE_3)).when(ingestionFilterer).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        doReturn(METADATA).when(metadataResolver).resolve(any(CvpItem.class));

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(1)).findByFolder(FOLDER_ONE);
        verify(hrsApiClient, times(1)).getIngestedFiles(FOLDER_ONE);
        verify(ingestionFilterer, times(1)).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        verify(metadataResolver, times(1)).resolve(any(CvpItem.class));
    }

    @Test
    void testShouldIngestMultipleFolders() throws Exception {
        doReturn(Set.of(FOLDER_ONE, FOLDER_TWO, FOLDER_THREE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(anyString());
        doReturn(HRS_FILESET_OF_2_FILES).when(hrsApiClient).getIngestedFiles(anyString());
        doReturn(Set.of(CVP_FILE_3)).when(ingestionFilterer).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        doReturn(METADATA).when(metadataResolver).resolve(any(CvpItem.class));

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(3)).findByFolder(anyString());
        verify(hrsApiClient, times(3)).getIngestedFiles(anyString());
        verify(ingestionFilterer, times(3)).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        verify(metadataResolver, times(3)).resolve(any(CvpItem.class));
    }



    @Test
    void testShouldRefuseIngestionWhenGettingFilesFromHrsApiRaisesInputOutException() throws Exception {
        doReturn(Set.of(FOLDER_ONE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(anyString());
        doThrow(IOException.class).when(hrsApiClient).getIngestedFiles(anyString());

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(1)).findByFolder(anyString());
        verify(hrsApiClient, times(1)).getIngestedFiles(anyString());
        verify(ingestionFilterer, never()).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        verify(metadataResolver, never()).resolve(any(CvpItem.class));
    }

    @Test
    void testShouldRefuseIngestionWhenGettingFilesFromHrsApiRaisesHrsApiException()
        throws Exception {
        doReturn(Set.of(FOLDER_ONE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(anyString());
        doThrow(HrsApiException.class).when(hrsApiClient).getIngestedFiles(anyString());

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(1)).findByFolder(anyString());
        verify(hrsApiClient, times(1)).getIngestedFiles(anyString());
        verify(ingestionFilterer, never()).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        verify(metadataResolver, never()).resolve(any(CvpItem.class));
    }

    @Test
    void testShouldRefuseIngestionWhenPostingFileToHrsRaiseHrsApiException() throws Exception {
        doReturn(Set.of(FOLDER_ONE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(anyString());
        doReturn(HRS_FILESET_OF_2_FILES).when(hrsApiClient).getIngestedFiles(anyString());
        doReturn(Set.of(CVP_FILE_3)).when(ingestionFilterer).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        doReturn(METADATA).when(metadataResolver).resolve(any(CvpItem.class));
        doThrow(HrsApiException.class).when(hrsApiClient).postFile(any(Metadata.class));

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(1)).findByFolder(anyString());
        verify(hrsApiClient, times(1)).getIngestedFiles(anyString());
        verify(ingestionFilterer, times(1)).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        verify(metadataResolver, times(1)).resolve(any(CvpItem.class));
    }

    @Test
    void testShouldRefuseIngestionWhenParsingFileNameRaisesFileParsingException() throws Exception {
        doReturn(Set.of(FOLDER_ONE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(anyString());
        doReturn(HRS_FILESET_OF_2_FILES).when(hrsApiClient).getIngestedFiles(anyString());
        doReturn(Set.of(CVP_FILE_3)).when(ingestionFilterer).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        doThrow(FilenameParsingException.class).when(metadataResolver).resolve(any(CvpItem.class));

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(1)).findByFolder(anyString());
        verify(hrsApiClient, times(1)).getIngestedFiles(anyString());
        verify(ingestionFilterer, times(1)).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_2_FILES);
        verify(hrsApiClient, never()).postFile(any(Metadata.class));
    }

    @Test
    void testShouldStopIngestingWhenBatchProcessingLimitReached() throws Exception {
        underTest.setMaxFilesToProcess(2);
        doReturn(Set.of(FOLDER_ONE)).when(cvpBlobstoreClient).getFolders();
        doReturn(CVP_ITEMSET_OF_3_FILES).when(cvpBlobstoreClient).findByFolder(FOLDER_ONE);
        doReturn(HRS_FILESET_OF_0_FILES).when(hrsApiClient).getIngestedFiles(FOLDER_ONE);
        doReturn(CVP_FILES_1_2_3_AS_SET).when(ingestionFilterer).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_0_FILES);
        doReturn(METADATA).when(metadataResolver).resolve(any(CvpItem.class));

        underTest.ingest();

        verify(cvpBlobstoreClient, times(1)).getFolders();
        verify(cvpBlobstoreClient, times(1)).findByFolder(FOLDER_ONE);
        verify(hrsApiClient, times(1)).getIngestedFiles(FOLDER_ONE);
        verify(ingestionFilterer, times(1)).filter(CVP_ITEMSET_OF_3_FILES, HRS_FILESET_OF_0_FILES);
        verify(metadataResolver, times(2)).resolve(any(CvpItem.class));
    }

}
