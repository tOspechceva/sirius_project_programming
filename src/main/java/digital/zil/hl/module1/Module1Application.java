package digital.zil.hl.module1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа Spring Boot приложения.
 */
@SpringBootApplication
@EnableScheduling
@EnableKafka
public class Module1Application {
    public static void main(final String[] args) {
        SpringApplication.run(Module1Application.class, args);
    }
}
