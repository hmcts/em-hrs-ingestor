package uk.gov.hmcts.reform.em.hrs.ingestor.parse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

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
    void parse_vh_file_name_with_interpreter_incasesensitive() throws FilenameParsingException {

        String dateStr = "2023-10-04-14.56.39.819";
        String timeZone = "UTC";
        DateTimeFormatter datePattern =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS").withZone(ZoneId.of(timeZone));
        LocalDateTime dateTimeObject = LocalDateTime.parse(dateStr, datePattern);
        UUID uniqueIdentifier = UUID.randomUUID();

        String fileName = "AA1-case-1/3-" + uniqueIdentifier + "_inTerpreter_6586_" + dateStr + "-UTC_4";
        ParsedFilenameDto parsed = VhFileNameParser.parseFileName(fileName);
        assertThat(parsed.getServiceCode()).isEqualTo("AA1");
        assertThat(parsed.getCaseID()).isEqualTo("case-1/3");
        assertThat(parsed.getUniqueIdentifier()).isEqualTo(uniqueIdentifier.toString());
        assertThat(parsed.getRecordingDateTime()).isEqualTo(dateTimeObject);
        assertThat(parsed.getSegment()).isEqualTo("4");
        assertThat(parsed.getInterpreter()).isEqualTo("inTerpreter_6586");
    }

    @Test
    void parse_vh_file_name_with_Interpreter() throws FilenameParsingException {

        String dateStr = "2023-10-04-14.56.39.819";
        String timeZone = "UTC";
        DateTimeFormatter datePattern =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS").withZone(ZoneId.of(timeZone));
        LocalDateTime dateTimeObject = LocalDateTime.parse(dateStr, datePattern);
        UUID uniqueIdentifier = UUID.randomUUID();

        String fileName = "AA1-caseref123312-" + uniqueIdentifier + "_Interpreter_1_" + dateStr + "-UTC_1";
        ParsedFilenameDto parsed = VhFileNameParser.parseFileName(fileName);
        assertThat(parsed.getServiceCode()).isEqualTo("AA1");
        assertThat(parsed.getCaseID()).isEqualTo("caseref123312");
        assertThat(parsed.getUniqueIdentifier()).isEqualTo(uniqueIdentifier.toString());
        assertThat(parsed.getRecordingDateTime()).isEqualTo(dateTimeObject);
        assertThat(parsed.getSegment()).isEqualTo("1");
        assertThat(parsed.getInterpreter()).isEqualTo("Interpreter_1");
    }

    @Test
    void parse_vh_file_name_throws_error_if_caseref_more_than_250() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-caseREF000"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567"
            + "8901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890-"
            + uniqueIdentifier
            + dateStr
            + "-UTC_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(
                fileName));
    }

    @Test
    void parse_vh_file_name_throws_error_if_caseref_less_than_1() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1--"
            + uniqueIdentifier
            + dateStr
            + "-UTC_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(
                fileName));
    }

    @Test
    void parse_vh_file_name_throws_error_if_caseref_has_folder() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "folder/AA1-caseref-"
            + uniqueIdentifier
            + dateStr
            + "-UTC_1";
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(
                fileName));
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideInvalidFileNamesAndTestNames")
    void parse_vh_file_name_throws_error_for_invalid_file_names(String testName, String fileName) {
        assertThatExceptionOfType(FilenameParsingException.class)
            .isThrownBy(() -> VhFileNameParser.parseFileName(fileName));
    }

    private static Stream<Arguments> provideInvalidFileNamesAndTestNames() {
        return Stream.of(
            Arguments.of(
                "parse_vh_file_name_throws_error_if_dateTime_wrong",
                "AA1-case-1/3-acde070d-" + UUID.randomUUID() + "_2023-13-04-14.56.32.819-UTC_1"
            ),
            Arguments.of(
                "parse_vh_file_name_throws_error_if_zoneMissing",
                "AA1-case-1/3-acde070d-" + UUID.randomUUID() + "_2023-13-04-14.56.32.819_1"
            ),
            Arguments.of(
                "parse_vh_file_name_throws_error_if_segment_missing",
                "AA1-case-1/3-acde070d-" + UUID.randomUUID() + "_2023-13-04-14.56.32.819-UTC"
            )
        );
    }

    @Test
    void isValid_vh_file_name_return_true() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-caseref123312-" + uniqueIdentifier + dateStr + "-UTC_1";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isTrue();
    }

    @Test
    void isValid_vh_file_name_return_false_if_extension_not_mp() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-caseref123312-" + uniqueIdentifier + dateStr + "-UTC_1.txt";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isFalse();
    }

    @Test
    void isValid_vh_file_name_return_true_there_is_extension() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-caseref123312-" + uniqueIdentifier + dateStr + "-UTC_1.mp4";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isTrue();
    }

    @Test
    void isValid_vh_file_name_with_Interpreter_return_true() {
        String dateStr = "2023-10-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-caseref123312-" + uniqueIdentifier + "_Interpreter_1_" + dateStr + "-UTC_1";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isTrue();
    }

    @Test
    void isValid_vh_file_name_return_false_if_uuid_less_than_36() {
        String fileName = "AA1-case-1/3-acde070d-8c4c-4f0d-9d8a-162843c1033_2023-11-04-14.56.32.819-UTC_1";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isFalse();
    }

    @Test
    void isValid_vh_file_name_return_false_if_zoneMissing() {
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-case-1/3-acde070d-" + uniqueIdentifier + "_2023-13-04-14.56.32.819_1";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isFalse();
    }

    @Test
    void isValid_vh_file_name_return_false_if_segment_missing() {
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-case-1/3-acde070d-" + uniqueIdentifier + "_2023-13-04-14.56.32.819-UTC";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isFalse();
    }


    @Test
    void pisValid_vh_file_name_throws_error_if_filename_more_than_255() {
        String dateStr = "_2024-11-04-14.56.39.819";
        UUID uniqueIdentifier = UUID.randomUUID();
        String fileName = "AA1-caseREF123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567-"
            + uniqueIdentifier
            + dateStr
            + "-UTC_1";
        assertThat(VhFileNameParser.isValidFileName(fileName)).isFalse();
    }

}
