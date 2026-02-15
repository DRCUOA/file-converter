package app.core;

import com.cloudconvert.dto.response.ErrorResponse;
import com.cloudconvert.dto.result.Status;
import com.cloudconvert.exception.CloudConvertException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds concise user-facing error messages from thrown exceptions.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static String fromException(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        CloudConvertException cloudConvertException = findCloudConvertException(throwable);
        if (cloudConvertException != null) {
            return fromCloudConvert(cloudConvertException);
        }
        String message = firstNonBlank(throwable.getMessage());
        if (message != null) {
            return message;
        }
        Throwable root = rootCause(throwable);
        if (root != throwable) {
            String rootMessage = firstNonBlank(root.getMessage());
            if (rootMessage != null) {
                return rootMessage;
            }
        }
        return throwable.getClass().getSimpleName();
    }

    static String fromCloudConvert(CloudConvertException exception) {
        List<String> details = new ArrayList<>();
        Status status = exception.getStatus();
        if (status != null && status.getCode() > 0) {
            String reason = firstNonBlank(status.getReason());
            details.add(reason == null
                    ? "HTTP " + status.getCode()
                    : "HTTP " + status.getCode() + " " + reason);
        }
        ErrorResponse body = exception.getBody();
        if (body != null) {
            String code = firstNonBlank(body.getCode());
            if (code != null) {
                details.add("code=" + code);
            }
            String message = firstNonBlank(body.getMessage());
            if (message != null) {
                details.add(message);
            }
            Map<String, Object> errors = body.getErrors();
            if (errors != null && !errors.isEmpty()) {
                details.add("details=" + errors);
            }
        }
        if (details.isEmpty()) {
            return "CloudConvert request failed";
        }
        return "CloudConvert request failed: " + String.join(" | ", details);
    }

    private static CloudConvertException findCloudConvertException(Throwable throwable) {
        Throwable current = throwable;
        int maxDepth = 12;
        for (int i = 0; i < maxDepth && current != null; i++) {
            if (current instanceof CloudConvertException cloudConvertException) {
                return cloudConvertException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        int maxDepth = 12;
        for (int i = 0; i < maxDepth && current.getCause() != null; i++) {
            current = current.getCause();
        }
        return current;
    }

    private static String firstNonBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
