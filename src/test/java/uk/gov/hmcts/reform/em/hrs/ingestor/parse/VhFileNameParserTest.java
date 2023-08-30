package uk.gov.hmcts.reform.em.hrs.ingestor.parse;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class VhFileNameParserTest {

    @Test
    void parse_vh_file_name() throws FilenameParsingException {

        String dateStr = "2023-11-04-14.56.32.819";
        String timeZone = "UTC";
        DateTimeFormatter datePattern =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS").withZone(ZoneId.of(timeZone));
        LocalDateTime dateTimeObject = LocalDateTime.parse(dateStr, datePattern);

        String fileName = "AA1-21-1/case-12-acde070d-8c4c-4f0d-9d8a-162843c10333_" + dateStr + "-UTC_1";
        ParsedFilenameDto parsed = VhFileNameParser.parseFileName(fileName);
        assertThat(parsed.getServiceCode()).isEqualTo("AA1");
        assertThat(parsed.getCaseID()).isEqualTo("21-1/case-12");
        assertThat(parsed.getUniqueIdentifier()).isEqualTo("acde070d-8c4c-4f0d-9d8a-162843c10333");
        assertThat(parsed.getRecordingDateTime()).isEqualTo(dateTimeObject);
        assertThat(parsed.getSegment()).isEqualTo("1");
        assertThat(parsed.getInterpreter()).isNull();
    }

    @Test
    void parse_vh_file_name_with_interpreter() throws FilenameParsingException {

        String dateStr = "2023-10-04-14.56.39.819";
        String timeZone = "UTC";
        DateTimeFormatter datePattern =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS").withZone(ZoneId.of(timeZone));
        LocalDateTime dateTimeObject = LocalDateTime.parse(dateStr, datePattern);
        UUID uniqueIdentifier = UUID.randomUUID();

        String fileName = "AA1-case-1/3-" + uniqueIdentifier + "_inTerpreter1_" + dateStr + "-UTC_4";
        ParsedFilenameDto parsed = VhFileNameParser.parseFileName(fileName);
        assertThat(parsed.getServiceCode()).isEqualTo("AA1");
        assertThat(parsed.getCaseID()).isEqualTo("case-1/3");
        assertThat(parsed.getUniqueIdentifier()).isEqualTo(uniqueIdentifier.toString());
        assertThat(parsed.getRecordingDateTime()).isEqualTo(dateTimeObject);
        assertThat(parsed.getSegment()).isEqualTo("4");
    }

    @Test
    void parse_vh_file_name_throws_error_if_uuid_less_than_36() {
        String fileName = "AA1-case-1/3-acde070d-8c4c-4f0d-9d8a-162843c1033_2023-11-04-14.56.32.819-UTC_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(
            fileName));
    }

    @Test
    void parse_vh_file_name_throws_error_if_uuid_more_than_36() {
        String fileName = "AA1-case-1/3-acde070d-8c4c-4f0d-9d8a-162843c1033233_2023-11-04-14.56.32.819-UTC_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(fileName));
    }

    @Test
    void parse_vh_file_name_throws_error_if_dateTime_wrong() {
        UUID uniqueIdentifier = UUID.randomUUID();

        String fileName = "AA1-case-1/3-acde070d-" + uniqueIdentifier + "_2023-13-04-14.56.32.819-UTC_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(fileName));
    }

    @Test
    void parse_vh_file_name_throws_error_if_zoneMissing() {
        UUID uniqueIdentifier = UUID.randomUUID();

        String fileName = "AA1-case-1/3-acde070d-" + uniqueIdentifier + "_2023-13-04-14.56.32.819_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(fileName));
    }

    @Test
    void parse_vh_file_name_throws_error_if_segment_missing() {
        UUID uniqueIdentifier = UUID.randomUUID();

        String fileName = "AA1-case-1/3-acde070d-" + uniqueIdentifier + "_2023-13-04-14.56.32.819-UTC";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(fileName));
    }

}
