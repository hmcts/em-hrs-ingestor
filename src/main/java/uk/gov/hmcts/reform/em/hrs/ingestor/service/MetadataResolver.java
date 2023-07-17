package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import uk.gov.hmcts.reform.em.hrs.ingestor.exception.FilenameParsingException;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;

public interface MetadataResolver {

    MetadataResolverImpl.FileLocationAndParts extractFileLocationAndParts(String filenameWithPath);

    Metadata resolve(SourceBlobItem item) throws FilenameParsingException;

}
