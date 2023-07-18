package uk.gov.hmcts.reform.em.hrs.ingestor.util;

import java.time.ZoneId;

public class DateUtil {
    private static final String EUROPE_LONDON = "Europe/London";
    public static final ZoneId EUROPE_LONDON_ZONE_ID = ZoneId.of(EUROPE_LONDON);

    private DateUtil() {
    }
}
