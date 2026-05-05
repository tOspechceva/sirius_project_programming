package digital.zil.hl.module1.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Потребитель команд из Kafka (основное приложение, LAB12).
 *
 * <p>{@code concurrency} задаёт число потоков consumer в одном процессе; чтобы они
 * реально работали параллельно по одному топику, число партиций топика должно быть
 * не меньше concurrency (см. задание LAB12).
 */
@Component
public class CourseKafkaListener {
    private static final Logger LOG = LoggerFactory.getLogger(CourseKafkaListener.class);

    private final KafkaCommandProcessor kafkaCommandProcessor;

    public CourseKafkaListener(final KafkaCommandProcessor kafkaCommandProcessor) {
        this.kafkaCommandProcessor = kafkaCommandProcessor;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.commands}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${spring.kafka.listener.concurrency}"
    )
    public void onCommandBatch(final List<String> values) {
        LOG.debug("Kafka batch received, size={}", values.size());
        for (final String value : values) {
            kafkaCommandProcessor.handle(value);
        }
    }
}
