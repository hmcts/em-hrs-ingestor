package uk.gov.hmcts.reform.em.hrs.ingestor.model;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class CvpItem {
    @Nonnull
    private final String filename;
    private final String fileUri;
    private final String md5Hash;
    private final long contentLength;
}
