package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.SourceBlobItem;

import java.util.Set;

public interface IngestionFilterer {

    Set<SourceBlobItem> filter(final CvpItemSet cvpItemSet, final HrsFileSet hrsFileSet);

}
