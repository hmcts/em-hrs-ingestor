package uk.gov.hmcts.reform.em.hrs.ingestor.exception;

import lombok.Getter;
import okhttp3.ResponseBody;

@Getter
public class HrsApiException extends Exception {
    private final int code;
    private final String message;
    private final String body;

    public HrsApiException(String message) {
        this(0, message, null);
    }

    public HrsApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = 0;
        this.message = message;
        this.body = null;
    }

    public HrsApiException(int code, String message, ResponseBody body) {
        this.code = code;
        this.message = message;
        this.body = body != null ? body.toString() : null;
    }
}
