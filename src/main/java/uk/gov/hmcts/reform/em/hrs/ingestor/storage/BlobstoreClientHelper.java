package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HearingSource;

import java.util.Set;

public interface BlobstoreClientHelper {
    HearingSource getHearingSource();

    Set<String> getFolders();

    CvpItemSet findByFolder(String folderName);
}
