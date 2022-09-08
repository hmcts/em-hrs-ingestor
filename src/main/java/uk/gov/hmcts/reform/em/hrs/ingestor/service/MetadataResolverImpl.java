package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.parse.FilenameParser;

@Component
public class MetadataResolverImpl implements MetadataResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataResolverImpl.class);

    @Override
    public FileLocationAndParts extractFileLocationAndParts(String filenameWithPath) {
        LOGGER.debug("resolving filenameWithPath {}", filenameWithPath);
        final String folderPrefix = "^audiostream";
        final String[] splitOnForwardSlash = filenameWithPath.split("/");
        int fileNameAndFolderPartsCount = splitOnForwardSlash.length;
        LOGGER.debug("fileNameAndFolderPartsCount length: {}", fileNameAndFolderPartsCount);
        if (fileNameAndFolderPartsCount != 2) {
            LOGGER.warn("not valid filepath (required: folder/filename) for filename: {}", filenameWithPath);
            return null;
        }
        final String folder = splitOnForwardSlash[0];
        LOGGER.debug("folder: {}", folder);
        final String filenameWithExtension = splitOnForwardSlash[1];
        LOGGER.debug("filenameWithExtension: {}", filenameWithExtension);
        final int lastIndexOfPeriodCharacter = filenameWithExtension.lastIndexOf(".");

        String fileNamePart = filenameWithExtension.substring(0, lastIndexOfPeriodCharacter);
        String fileExtensionPart = filenameWithExtension.substring(lastIndexOfPeriodCharacter + 1);
        LOGGER.debug("fileNamePart {}", fileNamePart);
        LOGGER.debug("fileExtensionPart {}", fileExtensionPart);

        int roomNumber = Integer.parseInt(folder.replaceFirst(folderPrefix, ""));

        return new FileLocationAndParts(
            folder,
            roomNumber,
            fileNamePart,
            fileExtensionPart
        );
    }

    @Override
    public Metadata resolve(final CvpItem item) throws FilenameParsingException {

        String filename = item.getFilename();
        final FileLocationAndParts fragments = extractFileLocationAndParts(filename);
        if (fragments == null) {
            throw new FilenameParsingException(
                "Unable to extract filename and location from full path for file: " + filename);
        }

        final ParsedFilenameDto parsedDataDto = FilenameParser.parseFileName(fragments.getFilenamePart());

        String parsedSegmentNumber = parsedDataDto.getSegment();

        if (!NumberUtils.isParsable(parsedSegmentNumber)) {
            parsedSegmentNumber = "0";
        }

        Metadata metadata = new Metadata(
            fragments.getFolder(),
            filename,
            item.getFileUri(),
            item.getContentLength(),
            item.getMd5Hash(),
            parsedDataDto.getUniqueIdentifier(),
            Integer.parseInt(parsedSegmentNumber),
            fragments.getFilenameSuffix(),
            parsedDataDto.getRecordingDateTime(),
            parsedDataDto.getCaseID(),
            fragments.getRoomNumber(),
            parsedDataDto.getJurisdiction(),
            parsedDataDto.getLocationCode(),
            parsedDataDto.getServiceCode()
        );
        return metadata;

    }

    @AllArgsConstructor
    @Getter
    class FileLocationAndParts {
        String folder;
        Integer roomNumber;
        String filenamePart;
        String filenameSuffix;
    }
}
