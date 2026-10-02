package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Golden round-trips for the Jackson 2 HRS Retrofit mapper (kebab-case).
 * Proves Boot 4 dual-stack does not silently reshape HRS payloads.
 */
class HrsApiJacksonRoundTripTest {

    private static final LocalDateTime RECORDING_DATE_TIME = LocalDateTime.of(2020, 7, 16, 10, 7, 31);
    private static final Metadata METADATA = new Metadata(
        "folder-1",
        "recording-file-name",
        "recording-cvp-uri",
        1L,
        "I2foA30B==",
        "recording-ref",
        0,
        "mp4",
        RECORDING_DATE_TIME,
        "xyz",
        HearingSource.CVP,
        222,
        "AB",
        "C3",
        "AAA1",
        "interpreter"
    );

    private ObjectMapper hrsApiObjectMapper;

    @BeforeEach
    void setUp() {
        hrsApiObjectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        hrsApiObjectMapper.findAndRegisterModules();
        hrsApiObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

    @Test
    void metadataSerializesWithKebabCasePropertyNames() throws Exception {
        final String json = hrsApiObjectMapper.writeValueAsString(METADATA);

        assertThat(json)
            .contains(
                "\"source-blob-url\"",
                "\"file-size\"",
                "\"check-sum\"",
                "\"recording-ref\"",
                "\"filename-extension\"",
                "\"recording-date-time\"",
                "\"case-ref\"",
                "\"recording-source\"",
                "\"hearing-room-ref\"",
                "\"jurisdiction-code\"",
                "\"court-location-code\"",
                "\"service-code\""
            )
            .doesNotContain(
                "\"sourceBlobUrl\"",
                "\"fileSize\"",
                "\"checkSum\"",
                "\"recordingRef\"",
                "\"filenameExtension\"",
                "\"recordingDateTime\"",
                "\"caseRef\"",
                "\"recordingSource\"",
                "\"hearingRoomRef\"",
                "\"jurisdictionCode\"",
                "\"courtLocationCode\"",
                "\"serviceCode\""
            );
        assertThat(json).contains("\"recording-source\":\"CVP\"");
        assertThat(json).contains("\"folder\":\"folder-1\"");
    }

    @Test
    void metadataRoundTripsThroughKebabCaseJson() throws Exception {
        final String json = hrsApiObjectMapper.writeValueAsString(METADATA);
        final Metadata roundTripped = hrsApiObjectMapper.readValue(json, Metadata.class);

        assertThat(roundTripped.getFolder()).isEqualTo(METADATA.getFolder());
        assertThat(roundTripped.getFilename()).isEqualTo(METADATA.getFilename());
        assertThat(roundTripped.getSourceBlobUrl()).isEqualTo(METADATA.getSourceBlobUrl());
        assertThat(roundTripped.getFileSize()).isEqualTo(METADATA.getFileSize());
        assertThat(roundTripped.getCheckSum()).isEqualTo(METADATA.getCheckSum());
        assertThat(roundTripped.getRecordingRef()).isEqualTo(METADATA.getRecordingRef());
        assertThat(roundTripped.getSegment()).isEqualTo(METADATA.getSegment());
        assertThat(roundTripped.getFilenameExtension()).isEqualTo(METADATA.getFilenameExtension());
        assertThat(roundTripped.getRecordingDateTime()).isEqualTo(METADATA.getRecordingDateTime());
        assertThat(roundTripped.getCaseRef()).isEqualTo(METADATA.getCaseRef());
        assertThat(roundTripped.getRecordingSource()).isEqualTo(METADATA.getRecordingSource());
        assertThat(roundTripped.getHearingRoomRef()).isEqualTo(METADATA.getHearingRoomRef());
        assertThat(roundTripped.getJurisdictionCode()).isEqualTo(METADATA.getJurisdictionCode());
        assertThat(roundTripped.getCourtLocationCode()).isEqualTo(METADATA.getCourtLocationCode());
        assertThat(roundTripped.getServiceCode()).isEqualTo(METADATA.getServiceCode());
        assertThat(roundTripped.getInterpreter()).isEqualTo(METADATA.getInterpreter());
    }

    @Test
    void recordingFilenameDtoDeserializesKebabCaseFolderName() throws Exception {
        final String json = "{\"folder-name\":\"folder-1\",\"filenames\":[\"file.mp4\"]}";

        final RecordingFilenameDto dto = hrsApiObjectMapper.readValue(
            json,
            new TypeReference<RecordingFilenameDto>() {
            }
        );

        assertThat(dto.getFolderName()).isEqualTo("folder-1");
        assertThat(dto.getFilenames()).isEqualTo(Set.of("file.mp4"));
    }

    @Test
    void recordingFilenameDtoRejectsCamelCaseFolderName() {
        final String json = "{\"folderName\":\"folder-1\",\"filenames\":[\"file.mp4\"]}";

        assertThatThrownBy(() -> hrsApiObjectMapper.readValue(
            json,
            new TypeReference<RecordingFilenameDto>() {
            }
        ))
            .isInstanceOf(UnrecognizedPropertyException.class)
            .hasMessageContaining("folderName");
    }
}
