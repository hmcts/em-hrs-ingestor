package uk.gov.hmcts.reform.em.hrs.ingestor.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.gov.hmcts.reform.em.hrs.ingestor.parse.FilenameParser.processRawDatePart;

public class VhFileNameParser {

    private static final Logger log = LoggerFactory.getLogger(VhFileNameParser.class);

    private static final String FILE_NAME_REGEX
        = "^(\\w+)-(.*)-([0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})(?:_(?i)"
        + "(Interpreter_\\d+))?_(.*)-(\\w+)_(\\d+)$";
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(FILE_NAME_REGEX);

    private VhFileNameParser() {
    }

    public static ParsedFilenameDto parseFileName(String inputString) throws FilenameParsingException {

        Matcher matcher = FILE_NAME_PATTERN.matcher(inputString);
        try {
            if (matcher.matches()) {
                return ParsedFilenameDto
                    .builder()
                    .serviceCode(matcher.group(1))
                    .caseID(matcher.group(2))
                    .recordingDateTime(processRawDatePart(matcher.group(5), matcher.group(6)))
                    .segment(matcher.group(7))
                    .uniqueIdentifier(matcher.group(3))
                    .interpreter(matcher.group(4))
                    .build();
            }
        } catch (Exception ex) {
            log.error("VH file name parsing failed: file name {}", inputString);
        }

        log.info("Could not parse VH file name: file name {}", inputString);
        throw new FilenameParsingException("Bad VH file format: input " + inputString);
    }

    public static boolean isValidFileName(String inputString) {
        String fileNameWithoutExtension = inputString.replaceAll("\\.(mp[^\\.]+)$", "");
        Matcher matcher = FILE_NAME_PATTERN.matcher(fileNameWithoutExtension);
        return matcher.matches();
    }
}
