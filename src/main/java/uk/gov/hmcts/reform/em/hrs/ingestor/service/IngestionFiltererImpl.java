package uk.gov.hmcts.reform.em.hrs.ingestor.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItem;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.CvpItemSet;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.HrsFileSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IngestionFiltererImpl implements IngestionFilterer {
    private static final Function<Set<String>, Function<Set<String>, Set<String>>> FILTER = x -> y -> {
        Set<String> a = new HashSet<>(x);
        Set<String> b = new HashSet<>(y);

        return a.removeAll(b) ? Collections.unmodifiableSet(a) : x;
    };

    @Override
    public Set<CvpItem> filter(final CvpItemSet cvpItemSet, final HrsFileSet hrsFileSet) {
        final Set<String> filtered = FILTER.apply(cvpItemSet.getCvpFiles()).apply(hrsFileSet.getHrsFiles());

        return cvpItemSet.getCvpItems().stream()
            .filter(x -> filtered.contains(x.getFilename()))
            .collect(Collectors.toUnmodifiableSet());
    }
}
