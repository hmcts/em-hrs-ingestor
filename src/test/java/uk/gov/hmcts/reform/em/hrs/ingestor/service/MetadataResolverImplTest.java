package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.parse.FilenameParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class MetadataResolverImplTest {
    private static final String FILENAME_VALID =
        "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_0.mp4";
    private static final String FILENAME_NO_SEGMENT =
        "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC.mp4";

    private static final String FILENAME_INVALID_SEGMENT =
        "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_X.mp4";
    private static final String FILENAME_NO_FOLDER = "bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_X.mp4";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS");
    private static final SourceBlobItem CVP_ITEM = createCvpItem(FILENAME_VALID);
    private static final SourceBlobItem CVP_ITEM_NO_SEGMENT = createCvpItem(FILENAME_NO_SEGMENT);
    private static final SourceBlobItem CVP_ITEM_INVALID_SEGMENT = createCvpItem(FILENAME_INVALID_SEGMENT);
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
    void testShouldDefaultSegmentToZeroWhenNotParsable() throws FilenameParsingException {
        String filename = "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_XYZ.mp4";
        SourceBlobItem item = createCvpItem(filename);

        try (MockedStatic<FilenameParser> parserMock = mockStatic(FilenameParser.class)) {
            parserMock.when(() -> FilenameParser.parseFileName(anyString()))
                .thenReturn(new ParsedFilenameDto(
                    "case-id-123",
                    "loc-id-123",
                    "case-id-123",
                    LocalDateTime.now(),
                    "nonNumeric",
                    "uniqueIdentifier",
                    "room-ref-1",
                    "jurisdiction-123",
                    "interpreter-123"
                ));

            Metadata metadata = underTest.resolve(item);

            assertThat(metadata.getSegment()).isZero();
        }
    }

    @Test
    void testUnsupportedHearingSourceThrowsException() {
        String filename = "audiostream12/bp-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_1.mp4";
        SourceBlobItem nonCvpItem = new SourceBlobItem(
            filename,
            "file-uri",
            "a2B4==",
            123L,
            HearingSource.VH
        );

        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> underTest.resolve(nonCvpItem))
            .withMessageContaining("Unsupported hearing source");
    }

}
