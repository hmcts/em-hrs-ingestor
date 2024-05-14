package uk.gov.hmcts.reform.em.hrs.ingestor.parse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("This Test class is disabled as it executes close to 18000 Test "
    + "and may not be helpful for this to be executed in the pipeline")
class FilenameParserWithFullDataExtractTest {

    private static final Logger log = LoggerFactory.getLogger(FilenameParserWithFullDataExtractTest.class);

    @ParameterizedTest(name = "Self Evaluation File : {0} --> {1}")
    @CsvFileSource(resources
        = "/uk/gov/hmcts/reform/em/hrs/hrs/ingestor/data/"
        + "All_report_size_2020-11-06T16_32_41+0000- Analysis_With_Filename.csv",
        numLinesToSkip = 1)
    void test_input_file_input(final String fileName,
                               final String jurisdiction,
                               final String jurisdictionMatch,
                               final String locationCode,
                               final String locationMatch,
                               final String caseID) throws Exception {
        String inputValue = fileName.substring(0, fileName.lastIndexOf('.'));
        ParsedFilenameDto parsedFilenameDto = FilenameParser.parseFileName(inputValue);
        verifyValuesOfMappedResponse(
            parsedFilenameDto,
            jurisdiction,
            jurisdictionMatch,
            locationCode,
            locationMatch,
            caseID
        );
    }


    private void verifyValuesOfMappedResponse(final ParsedFilenameDto parsedFilenameDto,
                                              final String jurisdictionCode,
                                              final String jurisdictionMatch,
                                              final String locationCode,
                                              final String locationMatch,
                                              final String caseReference) {

        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.debug("Input Jurisdiction Code {}", jurisdictionCode);
        log.debug("Input Location Code {}", locationCode);
        log.debug("Input Case Reference {}", caseReference);

        if ((Objects.nonNull(jurisdictionMatch) && !jurisdictionMatch.isEmpty() && !jurisdictionMatch.isBlank()
            && jurisdictionMatch.equalsIgnoreCase("Yes"))
            && (Objects.nonNull(locationMatch) && !locationMatch.isEmpty() && !locationMatch.isBlank()
            && locationMatch.equalsIgnoreCase("Yes"))) {

            log.debug("The Complete Scenario");
            log.debug("The value of the Mapped Jurisdiction Code {}", parsedFilenameDto.getJurisdiction());
            log.debug("The value of the Mapped Location Code {}", parsedFilenameDto.getLocationCode());
            log.debug("The value of the Mapped Case ID {}", parsedFilenameDto.getCaseID());

            assertTrue(jurisdictionCode.equalsIgnoreCase(parsedFilenameDto.getJurisdiction().trim()));
            assertTrue(locationCode.equalsIgnoreCase(parsedFilenameDto.getLocationCode().trim()));

            assertTrue((caseReference == null ? "" : caseReference)
                           .equalsIgnoreCase(parsedFilenameDto.getCaseID().trim()));

        } else if (
            (Objects.nonNull(jurisdictionMatch) && !jurisdictionMatch.isEmpty() && !jurisdictionMatch.isBlank()
                && jurisdictionMatch.equalsIgnoreCase("Yes"))
                && (Objects.nonNull(locationMatch) && !locationMatch.isEmpty() && !locationMatch.isBlank()
                && !locationMatch.equalsIgnoreCase("Yes"))) {

            log.debug("The No Location Code Scenario");
            log.debug("The value of the Mapped Jurisdiction Code {}", parsedFilenameDto.getJurisdiction());
            log.debug("The value of the Mapped Case ID {}", parsedFilenameDto.getCaseID());

            assertNull(parsedFilenameDto.getLocationCode());
            assertTrue(jurisdictionCode.equalsIgnoreCase(parsedFilenameDto.getJurisdiction().trim()));
            assertTrue(caseReference.equalsIgnoreCase(parsedFilenameDto.getCaseID().trim()));

        } else {

            log.debug("The Only Case ID Scenario");
            assertNull(parsedFilenameDto.getJurisdiction());
            assertNull(parsedFilenameDto.getLocationCode());
            assertTrue(caseReference.equalsIgnoreCase(parsedFilenameDto.getCaseID()));
        }
        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }
}
