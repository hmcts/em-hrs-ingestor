package uk.gov.hmcts.reform.em.hrs.ingestor.storage;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class BlobHelper {

    private BlobHelper() {
    }

    public static final String NULLMD_5 = "NULLMD5";
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobHelper.class);

    @NotNull
    static String parseFolderFromPath(String filePath) {
        final int separatorIndex = filePath.indexOf("/");
        LOGGER.debug("Separator Index {}", separatorIndex);
        String folder = "";
        if (separatorIndex == -1) {
            LOGGER.warn("Invalid Path for filepath {} ", filePath);
        } else {
            folder = filePath.substring(0, separatorIndex);
        }
        LOGGER.debug("folder: {}", folder);
        return folder;
    }

    static String getMd5Hash(final byte[] digest) {
        if (digest == null) {
            return NULLMD_5;
        }
        return Base64.getEncoder().encodeToString(digest);
    }

}
