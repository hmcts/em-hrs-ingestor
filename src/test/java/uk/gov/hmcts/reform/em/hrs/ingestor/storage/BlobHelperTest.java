package uk.gov.hmcts.reform.em.hrs.ingestor.storage;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BlobHelperTest {

    @Test
    void testParsesBadFoldersWithoutExceptions() {
        Assertions.assertNotNull(BlobHelper.parseFolderFromPath(""));
        Assertions.assertNotNull(BlobHelper.parseFolderFromPath("folder"));
        Assertions.assertNotNull(BlobHelper.parseFolderFromPath("folder/"));
        Assertions.assertNotNull(BlobHelper.parseFolderFromPath("folder/with_filename.mp4"));
    }


    @Test
    void testParsesNullMD5WithoutExceptions() {
        String expected = BlobHelper.NULLMD_5;
        String actual = BlobHelper.getMd5Hash(null);
        Assertions.assertEquals(expected, actual);
    }

}
