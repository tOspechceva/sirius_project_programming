package digital.zil.hl.module1.config;

import digital.zil.hl.module1.repository.LessonProgressRepository;
import digital.zil.hl.module1.repository.LessonRepository;
import digital.zil.hl.module1.repository.UserRepository;
import digital.zil.hl.module1.service.CompletionProgressCalculator;
import digital.zil.hl.module1.service.CourseProgressService;
import digital.zil.hl.module1.service.ProgressCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Явно объявляем бины приложения.
 *
 * <p>Конкретные реализации репозиториев предоставляются Spring Data JPA
 * адаптерами из пакета repository.jpa.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public ProgressCalculator progressCalculator() {
        return new CompletionProgressCalculator();
    }

    @Bean
    public CourseProgressService courseProgressService(
            final UserRepository userRepository,
            final LessonRepository lessonRepository,
            final LessonProgressRepository lessonProgressRepository,
            final ProgressCalculator progressCalculator
    ) {
        return new CourseProgressService(
                userRepository,
                lessonRepository,
                lessonProgressRepository,
                progressCalculator
        );
    }
}
