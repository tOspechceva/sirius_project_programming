package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.model.LessonProgress;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий прогресса обучения.
 *
 * <p>Сущность прогресса является связью многие-ко-многим между пользователем и уроком.
 */
public interface LessonProgressRepository {
    LessonProgress completeLesson(long userId, long lessonId, LocalDate completionDate, int testResult);

    Optional<LessonProgress> findByUserIdAndLessonId(long userId, long lessonId);

    List<LessonProgress> findByUserId(long userId);

    List<LessonProgress> findByLessonId(long lessonId);

    boolean deleteByUserIdAndLessonId(long userId, long lessonId);

    int deleteByUserId(long userId);

    int deleteByLessonId(long lessonId);

    List<LessonProgress> findAll();
}
