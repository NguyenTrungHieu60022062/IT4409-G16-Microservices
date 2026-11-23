package vn.it4409.g16.user.web;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ trả cho client: mã ổn định (code) + thông điệp an toàn để hiển thị. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;
    private final Long retryAfterSeconds;

    public ApiException(HttpStatus status, String code, String message, List<String> details, Long retryAfterSeconds) {
        super(message, null, false, false); // không cần stack trace cho lỗi nghiệp vụ
        this.status = status;
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiException of(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message, null, null);
    }

    public static ApiException badRequest(String code, String message, List<String> details) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message, details, null);
    }

    public static ApiException unauthorized(String code, String message) {
        return of(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException tooManyRequests(long retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                null, retryAfterSeconds);
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public List<String> details() { return details; }
    public Long retryAfterSeconds() { return retryAfterSeconds; }
}
