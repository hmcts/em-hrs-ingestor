package uk.gov.hmcts.reform.em.hrs.ingestor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Builder
public class ParsedFilenameDto {
    private final String jurisdiction;
    private final String locationCode;
    private final String caseID;
    private final LocalDateTime recordingDateTime;
    private final String segment;
    private final String uniqueIdentifier;
    private final String roomRef;
    private final String serviceCode;
}
