package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class MetadataResolverImplTest {
    private static final String FILENAME_VALID =
        "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_0.mp4";

    private static final String VH_FILENAME_VALID =
        "bp-0266-caseid-12-a-d-8b5dd35d-3a92-4bf3-8b6f-3196bed17014_2020-07-16-10.07.31.680-UTC_0.mp4";
    private static final String FILENAME_NO_SEGMENT =
        "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC.mp4";

    private static final String VH_FILENAME_NO_SEGMENT =
        "bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC.mp4";
    private static final String FILENAME_INVALID_SEGMENT =
        "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_X.mp4";
    private static final String VH_FILENAME_INVALID_SEGMENT =
        "bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_X.mp4";

    private static final String FILENAME_NO_FOLDER = "bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_X.mp4";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS");
    private static final SourceBlobItem CVP_ITEM = createCvpItem(FILENAME_VALID);
    private static final SourceBlobItem VH_ITEM = createVhItem(VH_FILENAME_VALID);
    private static final SourceBlobItem CVP_ITEM_NO_SEGMENT = createCvpItem(FILENAME_NO_SEGMENT);
    private static final SourceBlobItem VH_ITEM_NO_SEGMENT = createCvpItem(VH_FILENAME_NO_SEGMENT);
    private static final SourceBlobItem CVP_ITEM_INVALID_SEGMENT = createCvpItem(FILENAME_INVALID_SEGMENT);
    private static final SourceBlobItem VH_ITEM_INVALID_SEGMENT = createCvpItem(VH_FILENAME_INVALID_SEGMENT);
    private static final SourceBlobItem CVP_ITEM_NO_FOLDER = createCvpItem(FILENAME_NO_FOLDER);
    private final MetadataResolver underTest = new MetadataResolverImpl();

    @NotNull
    private static SourceBlobItem createCvpItem(String fileName) {
        return new SourceBlobItem(
            fileName,
            "file-uri",
            "a2B4==",
            123L,
            HearingSource.CVP
        );
    }

    private static SourceBlobItem createVhItem(String fileName) {
        return new SourceBlobItem(
            fileName,
            "file-uri-vh",
            "a2B423232==",
            201L,
            HearingSource.VH
        );
    }

    @Test
    void testFilenameWithoutFolderInPathThrowsFileParsingException() {
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> underTest.resolve(CVP_ITEM_NO_FOLDER));
    }

    @Test
    void testFilenameWithoutSegmentThrowsFileParsingException() {
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> underTest.resolve(CVP_ITEM_NO_SEGMENT));
    }

    @Test
    void testFilenameWithInvalidSegmentThrowsFileParsingException() {
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> underTest.resolve(CVP_ITEM_INVALID_SEGMENT));
    }

    @Test
    void testShouldResolveMetadataFromLoadTestFilename() throws FilenameParsingException {
        String input = "audiostream1562/LoadTest8562_2020-06-24-14.40.22.991-UTC_0.mp4";
        final Metadata metadata = underTest.resolve(createCvpItem(input));

        assertThat(metadata).satisfies(x -> {
            assertThat(x.getRecordingDateTime()).isEqualTo(LocalDateTime.parse("2020-06-24-14.40.22.991", FORMATTER));
            assertThat(x.getSegment()).isZero();
        });
    }

    @Test
    void testShouldResolveMetadataFromFilename() throws FilenameParsingException {
        final Metadata metadata = underTest.resolve(CVP_ITEM);

        assertThat(metadata).satisfies(x -> {
            assertThat(x.getRecordingDateTime()).isEqualTo(LocalDateTime.parse("2020-07-16-10.07.31.680", FORMATTER));
            assertThat(x.getSegment()).isZero();
        });
    }

    @Test
    void testShouldReturnRoomReference() {
        final MetadataResolverImpl.FileLocationAndParts fragments =
            underTest.extractFileLocationAndParts(FILENAME_VALID);

        assertThat(fragments.getRoomNumber()).isEqualTo(12);
    }

    @Test
    void testShouldReturnFolder() {
        final MetadataResolverImpl.FileLocationAndParts fragments =
            underTest.extractFileLocationAndParts(FILENAME_VALID);

        assertThat(fragments.getFolder()).isEqualTo("audiostream12");
    }

    @Test
    void testShouldReturnRecordingReference() {
        final String expectedString = "bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_0";

        final MetadataResolverImpl.FileLocationAndParts fragments =
            underTest.extractFileLocationAndParts(FILENAME_VALID);

        assertThat(fragments.getFilenamePart()).isEqualTo(expectedString);
    }

    @Test
    void testShouldReturnFilenameExtension() {
        final String expectedString = "mp4";

        final MetadataResolverImpl.FileLocationAndParts fragments =
            underTest.extractFileLocationAndParts(FILENAME_VALID);

        assertThat(fragments.getFilenameSuffix()).isEqualTo(expectedString);
    }


    @Test
    void testShouldResolveVhMetadataFromFilename() throws FilenameParsingException {
        final Metadata metadata = underTest.resolve(VH_ITEM);

        assertThat(metadata).satisfies(x -> {
            assertThat(x.getRecordingDateTime()).isEqualTo(LocalDateTime.parse("2020-07-16-10.07.31.680", FORMATTER));
            assertThat(x.getSegment()).isZero();
            assertThat(x.getFolder()).isEqualTo("VH");
            assertThat(x.getFilename()).isEqualTo(VH_FILENAME_VALID);
            assertThat(x.getFileSize()).isEqualTo(201);
            assertThat(x.getSegment()).isEqualTo(0);
            assertThat(x.getHearingRoomRef()).isEqualTo(0);
            assertThat(x.getFilenameExtension()).isEqualTo("mp4");
            assertThat(x.getSourceBlobUrl()).isEqualTo("file-uri-vh");
            assertThat(x.getCheckSum()).isEqualTo("a2B423232==");
        });
    }

    @Test
    void testVhFilenameWithoutSegmentThrowsFileParsingException() {
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> underTest.resolve(VH_ITEM_NO_SEGMENT));
    }

    @Test
    void testVhFilenameWithInvalidSegmentThrowsFileParsingException() {
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> underTest.resolve(VH_ITEM_INVALID_SEGMENT));
    }

}
