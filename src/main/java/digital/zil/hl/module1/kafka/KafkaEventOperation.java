package digital.zil.hl.module1.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * Тип операции в сообщении Kafka (LAB12).
 */
public enum KafkaEventOperation {
    POST,
    DEL;

    @JsonCreator
    public static KafkaEventOperation fromJson(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("operation must not be blank");
        }
        return KafkaEventOperation.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
