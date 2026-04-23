package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.model.Lesson;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий уроков.
 */
public interface LessonRepository {
    /**
     * Создает урок вместе с параметрами теста.
     */
    Lesson create(String topic, int videoDurationMinutes, String testName, int maxTestScore);

    /**
     * Находит урок по ID.
     */
    Optional<Lesson> findById(long id);

    /**
     * Обновляет урок по ID.
     */
    Optional<Lesson> update(long id, String topic, int videoDurationMinutes, String testName, int maxTestScore);

    /**
     * Удаляет урок по ID.
     */
    boolean deleteById(long id);

    /**
     * Возвращает все уроки.
     */
    List<Lesson> findAll();
}
