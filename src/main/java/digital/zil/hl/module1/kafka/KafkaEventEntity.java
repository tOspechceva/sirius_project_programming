package digital.zil.hl.module1.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * Тип доменной сущности в сообщении Kafka (LAB12).
 */
public enum KafkaEventEntity {
    USER,
    LESSON,
    PROGRESS;

    @JsonCreator
    public static KafkaEventEntity fromJson(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("entity must not be blank");
        }
        return KafkaEventEntity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
