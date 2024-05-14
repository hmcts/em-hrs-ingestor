package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.dto.ParsedFilenameDto;
import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.parse.FilenameParser;
import uk.gov.hmcts.reform.em.hrs.ingestor.parse.VhFileNameParser;

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

        var filePart = getFileParts(filenameWithExtension);

        LOGGER.debug("fileNamePart {}", filePart.fileNamePart);
        LOGGER.debug("fileExtensionPart {}", filePart.extension);

        int roomNumber = Integer.parseInt(folder.replaceFirst(folderPrefix, ""));
        return new FileLocationAndParts(
            folder,
            roomNumber,
            filePart.fileNamePart,
            filePart.extension
        );
    }

    @Override
    public Metadata resolve(final SourceBlobItem item) throws FilenameParsingException {

        String filename = item.getFilename();

        FileLocationAndParts fragments;
        if (item.getHearingSource() == HearingSource.VH) {
            fragments = extractVhFile(filename);
        } else {
            fragments = extractFileLocationAndParts(filename);
        }
        if (fragments == null) {
            throw new FilenameParsingException(
                "Unable to extract filename and location from full path for file: " + filename);
        }

        ParsedFilenameDto parsedDataDto = null;
        if (item.getHearingSource() == HearingSource.CVP) {
            parsedDataDto = FilenameParser.parseFileName(fragments.getFilenamePart());
        } else if (item.getHearingSource() == HearingSource.VH) {
            parsedDataDto = VhFileNameParser.parseFileName(fragments.getFilenamePart());
        }

        String parsedSegmentNumber = parsedDataDto.getSegment();

        LOGGER.info(
            "Parsed file caseID :{}, UniqueIdentifier{} , ServiceCode{}, Jurisdiction{} ",
            parsedDataDto.getCaseID(),
            parsedDataDto.getUniqueIdentifier(),
            parsedDataDto.getServiceCode(),
            parsedDataDto.getJurisdiction()
        );

        if (!NumberUtils.isParsable(parsedSegmentNumber)) {
            parsedSegmentNumber = "0";
        }

        return new Metadata(
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
            item.getHearingSource(),
            fragments.getRoomNumber(),
            parsedDataDto.getJurisdiction(),
            parsedDataDto.getLocationCode(),
            parsedDataDto.getServiceCode(),
            parsedDataDto.getInterpreter()
        );
    }

    private FileLocationAndParts extractVhFile(String filename) {
        var filePart = getFileParts(filename);
        return new FileLocationAndParts(
            "VH",
            0,
            filePart.fileNamePart,
            filePart.extension
        );
    }

    @AllArgsConstructor
    @Getter
    class FileLocationAndParts {
        String folder;
        Integer roomNumber;
        String filenamePart;
        String filenameSuffix;
    }

    record FilePart(String fileNamePart, String extension) {
    }

    private FilePart getFileParts(String filenameWithExtension) {
        final int lastIndexOfPeriodCharacter = filenameWithExtension.lastIndexOf(".");

        String fileNamePart = filenameWithExtension.substring(0, lastIndexOfPeriodCharacter);
        String fileExtensionPart = filenameWithExtension.substring(lastIndexOfPeriodCharacter + 1);
        return new FilePart(fileNamePart, fileExtensionPart);
    }

}
