package uk.gov.hmcts.reform.em.hrs.ingestor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Metadata {
    private final String folder;
    private final String filename;
    private final String sourceBlobUrl;
    private final long fileSize;
    private final String checkSum;
    private final String recordingRef;
    private final int segment;
    private final String filenameExtension;
    private final LocalDateTime recordingDateTime;
    private final String caseRef;
    private final HearingSource recordingSource;
    private final int hearingRoomRef;
    private final String jurisdictionCode;
    private final String courtLocationCode;
    private final String serviceCode;
}
