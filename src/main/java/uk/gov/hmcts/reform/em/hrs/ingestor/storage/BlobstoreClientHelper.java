package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;

import java.util.Set;

public interface BlobstoreClientHelper {
    Set<String> getFolders();

    CvpItemSet findByFolder(String folderName);
}
