package uk.gov.hmcts.reform.em.hrs.ingestor.parse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilenameParserTest {

    private static final Logger log = LoggerFactory.getLogger(FilenameParserTest.class);


    @ParameterizedTest(name = "Invalid parameter test : {0} --> {1}")
    @CsvSource(value = {"Empty Value,''", "Spaced Value,' '", "Value Value,NIL"}, nullValues = "NIL")
    void test_negative_invalid_file_name_input(final String inputKey, final String inputValue) {
        try {
            FilenameParser.parseFileName(inputValue);
        } catch (FilenameParsingException parsingException) {
            assertEquals(
                "The argument passed is not valid",
                parsingException.getCause().getLocalizedMessage()
            );
        }
    }

    @ParameterizedTest(name = "Wrong Format File Names : {0} --> {1}")
    @CsvFileSource(resources = "/uk/gov/hmcts/reform/em/hrs/hrs/ingestor/data/invalid-file-format-values.csv",
        numLinesToSkip = 1)
    void test_negative_wrong_format_file_name_input(final String inputKey, final String inputValue)
        throws Exception {
        ParsedFilenameDto parsedFilenameDto = FilenameParser.parseFileName(inputValue);
        String withoutSegment = inputValue.substring(0, inputValue.lastIndexOf("_"));
        String withoutTimeZone = withoutSegment.substring(0, withoutSegment.lastIndexOf("_"));
        assertEquals(
            withoutTimeZone,
            parsedFilenameDto.getCaseID().trim()
        );
    }

    @ParameterizedTest(name = "Positive tests for Civil and Family : {0} --> {1}")
    @CsvSource(value = {
        "Valid File Name All Capital Case,AAA1-0150-GN00NT095_2020-07-29-09.03.40.236-UTC_0",
        "Valid File Case Reference Small Case,AAA1-0291-g20yj687_2020-09-01-09.14.47.314-UTC_0",
        "Valid File Case Reference Jurisdiction Lower Case,aba1-0144-CD0XT621_2020-10-20-14.05.10.150-UTC_0",
        "Valid File Case Reference Case Reference Mixed Case,AAA1-0150-GH0YN819Part1_2020-07-29-09.03.40.236-UTC_99",
        "Valid File Case Hyphenated 1,ABC2-0291-OX20P00022-Keene-v-England_2020-06-30-10.30.18.388-UTC_0",
        "Valid File Case Hyphenated 2,AAA1-0291-IP-2019-000175-DPA-v-Dagyanno_2020-07-02-09.23.23.994-UTC_10",
        "Valid File Location Code 3 Digits 1,ABC2-211-GU20C0090_2020-10-01-08.12.06.568-UTC_1",
        "Valid File Location Code 3 Digits 2,ABC3-005-13605371-AB_2020-09-10-13.18.39.768-UTC_20"},
        nullValues = "NIL")
    void test_positive_location_code_based_input_for_civil_and_family(final String inputKey,
                                                                      final String inputValue) throws Exception {

        ParsedFilenameDto parsedFilenameDto = FilenameParser.parseFileName(inputValue);
        switch (inputKey) {
            case "Valid File Name All Capital Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "AAA1", "150",
                                             "GN00NT095", "0",
                                             "AAA1-0150-GN00NT095_2020-07-29-09.03.40.236-UTC",
                                             2020, 7, 29,
                                             9, 3, 40, 236000000
                );
                break;
            case "Valid File Case Reference Small Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "AAA1", "291",
                                             "g20yj687", "0",
                                             "AAA1-0291-g20yj687_2020-09-01-09.14.47.314-UTC",
                                             2020, 9, 1,
                                             9, 14, 47, 314000000
                );
                break;
            case "Valid File Case Reference Jurisdiction Lower Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "aba1", "144",
                                             "CD0XT621", "0",
                                             "aba1-0144-CD0XT621_2020-10-20-14.05.10.150-UTC",
                                             2020, 10, 20,
                                             14, 5, 10, 150000000
                );
                break;
            case "Valid File Case Reference Case Reference Mixed Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "AAA1", "150",
                                             "GH0YN819Part1", "99",
                                             "AAA1-0150-GH0YN819Part1_2020-07-29-09.03.40.236-UTC",
                                             2020, 7, 29,
                                             9, 3, 40, 236000000
                );
                break;
            case "Valid File Case Hyphenated 1":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABC2", "291",
                                             "OX20P00022-Keene-v-England", "0",
                                             "ABC2-0291-OX20P00022-Keene-v-England_2020-06-30-10.30.18.388-UTC",
                                             2020, 6, 30,
                                             10, 30, 18, 388000000
                );
                break;
            case "Valid File Case Hyphenated 2":
                verifyValuesOfMappedResponse(parsedFilenameDto, "AAA1", "291",
                                             "IP-2019-000175-DPA-v-Dagyanno", "10",
                                             "AAA1-0291-IP-2019-000175-DPA-v-Dagyanno_2020-07-02-09.23.23.994-UTC",
                                             2020, 7, 2,
                                             9, 23, 23, 994000000
                );
                break;
            case "Valid File Location Code 3 Digits 1":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABC2", "211",
                                             "GU20C0090", "1",
                                             "ABC2-211-GU20C0090_2020-10-01-08.12.06.568-UTC", 2020, 10, 1,
                                             8, 12, 6, 568000000
                );
                break;
            case "Valid File Location Code 3 Digits 2":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABC3", "005",
                                             "13605371-AB", "20",
                                             "ABC3-005-13605371-AB_2020-09-10-13.18.39.768-UTC", 2020, 9, 10,
                                             13, 18, 39, 768000000
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid key request for the Tests ");
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 10 LINES
    @ParameterizedTest(name = "Positive tests for Tribunals : {0} --> {1}")
    @CsvSource(value = {
        "Valid File Name All Capital Case Hyphenated,ABC4-0127-HU-02785-2020_2020-07-16-10.07.31.680-UTC_0",
        "Valid File Name All Lower Case Hyphenated,abc4-0127-hu-02785-2020_2020-07-16-10.07.31.680-UTC_0",
        "Valid File Name All Capital Case,ABC4-HU-01234-2018_2020-09-19-10.50.20.150-UTC_12",
        "Valid File Name Case Reference Hyphenated 1,ABC4-EA-04983-2019-EA-05166-2019_2020-10-01-11.25.25.976-UTC_0",
        "Valid File Name Case Reference Hyphenated 2,"
            + "ABC4-HU-15010-2019-HU-15014-2019-HU-15015-2019_2020-10-22-13.43.14.176-UTC_99",
        "Valid File Name Case Reference Hyphenated Lower Case,"
            + "abc4-hu-15010-2019-hu-15014-2019-hu-15015-2019_2020-10-22-13.43.14.176-UTC_255"},
        nullValues = "NIL")
    void test_positive_case_reference_based_input_for_tribunals(final String inputKey, final String inputValue)
        throws Exception {

        ParsedFilenameDto parsedFilenameDto = FilenameParser.parseFileName(inputValue);
        switch (inputKey) {
            case "Valid File Name All Capital Case Hyphenated":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABC4", "127", "HU-02785-2020",
                                             "0", "ABC4-0127-HU-02785-2020_2020-07-16-10.07.31.680-UTC", 2020, 7, 16,
                                             10,7, 31, 680000000
                );
                break;
            case "Valid File Name All Lower Case Hyphenated":
                verifyValuesOfMappedResponse(parsedFilenameDto, "abc4", "127", "hu-02785-2020",
                                             "0", "abc4-0127-hu-02785-2020_2020-07-16-10.07.31.680-UTC", 2020, 7, 16,
                                             10,7, 31, 680000000
                );
                break;
            case "Valid File Name All Capital Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABC4", null, "HU-01234-2018",
                                             "12", "ABC4-HU-01234-2018_2020-09-19-10.50.20.150-UTC", 2020, 9, 19, 10,
                                             50, 20, 150000000
                );
                break;
            case "Valid File Name Case Reference Hyphenated 1":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABC4", null,
                                             "EA-04983-2019-EA-05166-2019", "0",
                                             "ABC4-EA-04983-2019-EA-05166-2019_2020-10-01-11.25.25.976-UTC",
                                             2020, 10, 1, 11,
                                             25, 25, 976000000
                );
                break;
            case "Valid File Name Case Reference Hyphenated 2":
                verifyValuesOfMappedResponse(
                    parsedFilenameDto,
                    "ABC4",
                    null,
                    "HU-15010-2019-HU-15014-2019-HU-15015-2019",
                    "99",
                    "ABC4-HU-15010-2019-HU-15014-2019-HU-15015-2019_2020-10-22-13.43.14.176-UTC",
                    2020,
                    10,
                    22,
                    13,
                    43,
                    14,
                    176000000
                );
                break;
            case "Valid File Name Case Reference Hyphenated Lower Case":
                verifyValuesOfMappedResponse(
                    parsedFilenameDto,
                    "abc4",
                    null,
                    "hu-15010-2019-hu-15014-2019-hu-15015-2019",
                    "255",
                    "abc4-hu-15010-2019-hu-15014-2019-hu-15015-2019_2020-10-22-13.43.14.176-UTC",
                    2020,
                    10,
                    22,
                    13,
                    43,
                    14,
                    176000000
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid key request for the Tests ");
        }
    }


    @ParameterizedTest(name = "Positive tests for Royal courts of Justice Location based : {0} --> {1}")
    @CsvSource(value = {
        "Valid File Name All Capital Case Location Code 0372,AAA1-0372-HU-02785-2020_2020-07-16-10.07.31.680-UTC_0",
        "Valid File Name All Lower Case Location Code 0266,Aaa1-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC_100",
        "Valid File Name All Lower Case Location Code 0372,aaa1-0372-074mc866_2020-09-10-10.21.54.116-UTC_76"},
        nullValues = "NIL")
    void test_positive_location_code_based_input_for_royal_courts_of_justice(final String inputKey,
                                                                             final String inputValue)
        throws Exception {

        ParsedFilenameDto parsedFilenameDto = FilenameParser.parseFileName(inputValue);
        switch (inputKey) {
            case "Valid File Name All Capital Case Location Code 0372":
                verifyValuesOfMappedResponse(parsedFilenameDto, "AAA1", "372",
                                             "HU-02785-2020", "0",
                                             "AAA1-0372-HU-02785-2020_2020-07-16-10.07.31.680-UTC", 2020, 7, 16,
                                             10, 7, 31, 680000000
                );
                break;
            case "Valid File Name All Lower Case Location Code 0266":
                verifyValuesOfMappedResponse(parsedFilenameDto, "Aaa1", "266",
                                             "hu-02785-2020", "100",
                                             "Aaa1-0266-hu-02785-2020_2020-07-16-10.07.31.680-UTC", 2020, 7, 16,
                                             10, 7, 31, 680000000
                );
                break;
            case "Valid File Name All Lower Case Location Code 0372":
                verifyValuesOfMappedResponse(parsedFilenameDto, "aaa1", "372",
                                             "074mc866", "76",
                                             "aaa1-0372-074mc866_2020-09-10-10.21.54.116-UTC", 2020, 9, 10,
                                             10, 21, 54, 116000000
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid key request for the Tests ");
        }
    }

    @ParameterizedTest(name = "Positive tests for Royal courts of Justice non Location based : {0} --> {1}")
    @CsvSource(value = {
        "Valid File Name All Capital Case Hyphenated,ABD5-CO-2020-01430_2020-09-12-15.00.12.765-UTC_0",
        "Valid File Name No Location Code,ABD5-QB-2017-002538_2020-10-06-15.20.16.562-UTC_25",
        "Valid File Name With Case Reference Lower Case,ABD5-QB-blahblahTest_2020-09-22-10.22.53.511-UTC_0",
        "Valid File Name All Capital Case,ABE6-FD20P00625_2020-10-20-09.28.09.765-UTC_99",
        "Valid File Name Case Reference Hyphenated,ABE7-PR-2020-0016-1008_2020-09-21-09.08.26.413-UTC_0",
        "Valid File Name All Lower Case Hyphenated Lower Case,abd5-0164-co-2020-01425_2020-10-23-13.02.08.818-UTC_100"},
        nullValues = "NIL")
    void test_positive_non_location_code_based_input_for_royal_courts_of_justice(final String inputKey,
                                                                                 final String inputValue)
        throws Exception {

        ParsedFilenameDto parsedFilenameDto = FilenameParser.parseFileName(inputValue);
        switch (inputKey) {
            case "Valid File Name All Capital Case Hyphenated":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABD5", null, "CO-2020-01430",
                                             "0", "ABD5-CO-2020-01430_2020-09-12-15.00.12.765-UTC", 2020, 9, 12, 15,
                                             0, 12, 765000000
                );
                break;
            case "Valid File Name No Location Code":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABD5", null, "QB-2017-002538",
                                             "25", "ABD5-QB-2017-002538_2020-10-06-15.20.16.562-UTC", 2020, 10, 6, 15,
                                             20, 16, 562000000
                );
                break;
            case "Valid File Name With Case Reference Lower Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABD5", null, "QB-blahblahTest",
                                             "0", "ABD5-QB-blahblahTest_2020-09-22-10.22.53.511-UTC", 2020, 9, 22, 10,
                                             22, 53, 511000000
                );
                break;
            case "Valid File Name All Capital Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABE6", null, "FD20P00625",
                                             "99", "ABE6-FD20P00625_2020-10-20-09.28.09.765-UTC", 2020, 10, 20, 9,
                                             28, 9, 765000000
                );
                break;
            case "Valid File Name Case Reference Hyphenated":
                verifyValuesOfMappedResponse(parsedFilenameDto, "ABE7", null, "PR-2020-0016-1008",
                                             "0", "ABE7-PR-2020-0016-1008_2020-09-21-09.08.26.413-UTC", 2020, 9, 21, 9,
                                             8, 26, 413000000
                );
                break;
            case "Valid File Name All Lower Case Hyphenated Lower Case":
                verifyValuesOfMappedResponse(parsedFilenameDto, "abd5", "164",
                                             "co-2020-01425",
                                             "100",
                                             "abd5-0164-co-2020-01425_2020-10-23-13.02.08.818-UTC",
                                             2020, 10, 23, 13,
                                             2, 8, 818000000
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid key request for the Tests ");
        }

    }


    private void verifyValuesOfMappedResponse(final ParsedFilenameDto parsedFilenameDto,
                                              final String serviceCode,
                                              final String locationCode,
                                              final String caseReference,
                                              final String segment,
                                              final String recordingUniqueIdentifier,
                                              final int year, final int month,
                                              final int day, final int hour,
                                              final int minute, final int second, final int nano) {
        assertEquals(serviceCode, parsedFilenameDto.getServiceCode());
        if (parsedFilenameDto.getLocationCode() != null) {
            assertEquals(locationCode, parsedFilenameDto.getLocationCode().trim());
        }
        assertEquals(caseReference, parsedFilenameDto.getCaseID());
        assertEquals(recordingUniqueIdentifier, parsedFilenameDto.getUniqueIdentifier());
        assertEquals(segment, parsedFilenameDto.getSegment());

        LocalDateTime localDateTime = parsedFilenameDto.getRecordingDateTime();
        log.debug("The value of the Returned Date Time" + localDateTime);
        assertEquals(year, localDateTime.getYear());
        assertEquals(month, localDateTime.getMonth().getValue());
        assertEquals(day, localDateTime.getDayOfMonth());
        assertEquals(hour, localDateTime.getHour());
        assertEquals(minute, localDateTime.getMinute());
        assertEquals(second, localDateTime.getSecond());
        assertEquals(nano, localDateTime.getNano());
    }
}
