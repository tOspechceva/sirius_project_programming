package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.model.Lesson;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий уроков.
 */
public interface LessonRepository {
    Lesson create(String topic, int videoDurationMinutes, String testName, int maxTestScore);

    Optional<Lesson> findById(long id);

    Optional<Lesson> update(long id, String topic, int videoDurationMinutes, String testName, int maxTestScore);

    boolean deleteById(long id);

    List<Lesson> findAll();
}
