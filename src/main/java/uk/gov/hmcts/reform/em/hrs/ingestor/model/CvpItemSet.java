package uk.gov.hmcts.reform.em.hrs.ingestor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class CvpItemSet {
    private final Set<SourceBlobItem> sourceBlobItems;

    public Set<String> getCvpFiles() {
        return sourceBlobItems.stream()
            .map(SourceBlobItem::getFilename)
            .collect(Collectors.toUnmodifiableSet());
    }
}
