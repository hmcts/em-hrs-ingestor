package uk.gov.hmcts.reform.em.hrs.ingestor.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilenameParser {

    private static final Logger log = LoggerFactory.getLogger(FilenameParser.class);

    private static final String ROYAL_COURTS_OF_JUSTICE_FILE_WITH_LOCATION_FORMAT_REGEX
        = "^([A-Z][A-Z][A-Z]\\d)-(0372|0266)-([A-Z0-9-]*)_([0-9-.]*)-([A-Z]{3})_([0-9]+)$";
    private static final String CIVIL_AND_FAMILY_FILE_FORMAT_REGEX
        = "^([A-Z][A-Z][A-Z]\\d)-([0-9]{3,4})-([A-Z0-9-]*)_([0-9-.]*)-([A-Z]{3})_([0-9]+)$";
    private static final String TRIBUNALS_FILE_FORMAT_REGEX
        = "^([A-Z][A-Z][A-Z]\\d)-([A-Z0-9-]*)_([0-9-.]*)-([A-Z]{3})_([0-9]+)$";
    private static final String ROYAL_COURTS_OF_JUSTICE_FILE_WITHOUT_LOCATION_FORMAT_REGEX
        = "^([A-Z][A-Z][A-Z]\\d)-([A-Z0-9-]*)_([0-9-.]*)-([A-Z]{3})_([0-9]+)$";

    private static final String MINIMAL_FORMAT_REGEX
        = "^(.*?)_([0-9-.]*)-([A-Z]{3})_([0-9]+)$";

    private FilenameParser() {
    }

    public static ParsedFilenameDto parseFileName(final String fileName) throws FilenameParsingException {

        log.debug("This input fileName : " + fileName);
        if (Objects.isNull(fileName) || fileName.isBlank() || fileName.isEmpty()) {
            throw new FilenameParsingException(
                "Invalid Filename",
                new IllegalArgumentException("The argument passed is not valid")
            );
        }
        Matcher royalCourtsOfJusticeWithLocationMatcher
            = Pattern.compile(
            ROYAL_COURTS_OF_JUSTICE_FILE_WITH_LOCATION_FORMAT_REGEX,
            Pattern.CASE_INSENSITIVE
        ).matcher(fileName);
        Matcher civilAndFamilyMatcher
            = Pattern.compile(
            CIVIL_AND_FAMILY_FILE_FORMAT_REGEX,
            Pattern.CASE_INSENSITIVE
        ).matcher(fileName);
        Matcher tribunalsMatcher
            = Pattern.compile(TRIBUNALS_FILE_FORMAT_REGEX, Pattern.CASE_INSENSITIVE).matcher(fileName);
        Matcher royalCourtsOfJusticeWithoutLocationMatcher
            = Pattern.compile(
            ROYAL_COURTS_OF_JUSTICE_FILE_WITHOUT_LOCATION_FORMAT_REGEX,
            Pattern.CASE_INSENSITIVE
        ).matcher(fileName);
        Matcher caseRefAndTimeStampOnlyMatcher
            = Pattern.compile(
            MINIMAL_FORMAT_REGEX,
            Pattern.CASE_INSENSITIVE
        ).matcher(fileName);

        return processMatcher(
            fileName,
            civilAndFamilyMatcher,
            tribunalsMatcher,
            royalCourtsOfJusticeWithLocationMatcher,
            royalCourtsOfJusticeWithoutLocationMatcher,
            caseRefAndTimeStampOnlyMatcher
        );
    }

    private static ParsedFilenameDto processMatcher(final String fileName,
                                                    final Matcher civilAndFamilyMatcher,
                                                    final Matcher tribunalsMatcher,
                                                    final Matcher royalCourtsOfJusticeWithLocationMatcher,
                                                    final Matcher royalCourtsOfJusticeWithoutLocationMatcher,
                                                    final Matcher caseRefAndTimeStampOnlyFormatMatcher)
        throws FilenameParsingException {

        if (royalCourtsOfJusticeWithLocationMatcher.matches()) {
            log.debug("This is a Royal Courts of Justice Locations based match");
            return processLocationMatcher(
                royalCourtsOfJusticeWithLocationMatcher);
        } else if (civilAndFamilyMatcher.matches()) {
            log.debug("This is a Civil and Family based match");
            return processLocationMatcher(civilAndFamilyMatcher);
        } else if (royalCourtsOfJusticeWithoutLocationMatcher.matches()) {
            log.debug("This is a Royal Courts of Justice Without Locations based match");
            return processNonLocationMatcher(royalCourtsOfJusticeWithoutLocationMatcher);
        } else if (tribunalsMatcher.matches()) {
            log.debug("This is a Tribunals based match");
            return processNonLocationMatcher(tribunalsMatcher);
        } else if (caseRefAndTimeStampOnlyFormatMatcher.matches()) {
            log.debug("Unable To match, looking for timepart and case ref");
            return processBadFormatMatcher(caseRefAndTimeStampOnlyFormatMatcher);
        } else {
            throw new FilenameParsingException("Bad format");
        }

    }

    private static ParsedFilenameDto processLocationMatcher(final Matcher matcher) {
        return ParsedFilenameDto
            .builder()
            .serviceCode(matcher.group(1))
            .locationCode(
                matcher.group(2).trim().length() == 4
                ? matcher.group(2).replaceFirst("^0*", "")
                : matcher.group(2))
            .caseID(matcher.group(3))
            .recordingDateTime(processRawDatePart(matcher.group(4), matcher.group(5)))
            .segment(matcher.group(6))
            .uniqueIdentifier(matcher.group(1)
                                  + "-" + matcher.group(2)
                                  + "-" + matcher.group(3)
                                  + "_" + matcher.group(4)
                                  + "-" + matcher.group(5))
            .build();
    }


    private static ParsedFilenameDto processNonLocationMatcher(
        final Matcher matcher) {
        return ParsedFilenameDto
            .builder()
            .serviceCode(matcher.group(1))
            .caseID(matcher.group(2))
            .recordingDateTime(processRawDatePart(matcher.group(3), matcher.group(4)))
            .segment(matcher.group(5))
            .uniqueIdentifier(matcher.group(1) + "-"
                                  + matcher.group(2)
                                  + "_" + matcher.group(3)
                                  + "-" + matcher.group(4))
            .build();

    }


    private static ParsedFilenameDto processBadFormatMatcher(
        final Matcher matcher) {
        return ParsedFilenameDto
            .builder()
            .caseID(matcher.group(1))
            .recordingDateTime(processRawDatePart(matcher.group(2), matcher.group(3)))
            .segment(matcher.group(4))
            .uniqueIdentifier(matcher.group(1) + "-"
                                  + matcher.group(2)
                                  + "_" + matcher.group(3)
                                  + "-" + matcher.group(4))
            .build();

    }

    public static LocalDateTime processRawDatePart(final String rawDatePart, final String timeZone) {

        log.debug("The Later File Part " + rawDatePart);
        log.debug("The Time Zone Part " + timeZone);

        DateTimeFormatter datePattern =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSS").withZone(ZoneId.of(timeZone));
        LocalDateTime dateTimeObject = LocalDateTime.parse(rawDatePart, datePattern);

        log.debug("The value of the Formatted Date Time Object" + dateTimeObject);
        return dateTimeObject;
    }

}
