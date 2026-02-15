package app.unit;

import app.core.ErrorMessages;
import com.cloudconvert.dto.result.Status;
import com.cloudconvert.exception.CloudConvertClientException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessagesTest {

    @Test
    void cloudConvertExceptionIncludesApiDetails() throws Exception {
        String body = """
                {
                  "code": "INVALID_DATA",
                  "message": "Unsupported input format",
                  "errors": {
                    "tasks.convert": {
                      "input_format": [
                        "is invalid"
                      ]
                    }
                  }
                }
                """;
        CloudConvertClientException ex = new CloudConvertClientException(
                Status.builder().code(422).reason("Unprocessable Entity").build(),
                Map.of(),
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
        );

        String message = ErrorMessages.fromException(ex);

        assertThat(message).contains("CloudConvert request failed");
        assertThat(message).contains("HTTP 422 Unprocessable Entity");
        assertThat(message).contains("code=INVALID_DATA");
        assertThat(message).contains("Unsupported input format");
        assertThat(message).contains("details=");
    }

    @Test
    void fallsBackToRootCauseMessageWhenWrapperHasNoMessage() {
        Throwable wrapped = new RuntimeException(null, new IllegalStateException("boom"));

        String message = ErrorMessages.fromException(wrapped);

        assertThat(message).isEqualTo("boom");
    }

    @Test
    void fallsBackToClassNameWhenNoMessageExists() {
        String message = ErrorMessages.fromException(new RuntimeException());

        assertThat(message).isEqualTo("RuntimeException");
    }
}
