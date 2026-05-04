package digital.zil.hl.module1.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Контракт сообщения из Kafka: сущность, операция и полезная нагрузка (JSON).
 *
 * <p>Пример:
 * <pre>{@code {"entity":"USER","operation":"POST","payload":{"login":"x","email":"a@b.c","registrationDate":"2024-01-01"}}}</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KafkaCommandEnvelope(
        KafkaEventEntity entity,
        KafkaEventOperation operation,
        JsonNode payload
) {
}
