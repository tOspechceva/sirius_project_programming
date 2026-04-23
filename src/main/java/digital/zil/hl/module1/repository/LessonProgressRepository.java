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
    /**
     * Сохраняет факт завершения урока пользователем.
     */
    LessonProgress completeLesson(long userId, long lessonId, LocalDate completionDate, int testResult);

    /**
     * Ищет прогресс по связке пользователь-урок.
     */
    Optional<LessonProgress> findByUserIdAndLessonId(long userId, long lessonId);

    /**
     * Возвращает весь прогресс конкретного пользователя.
     */
    List<LessonProgress> findByUserId(long userId);

    /**
     * Возвращает весь прогресс конкретного урока.
     */
    List<LessonProgress> findByLessonId(long lessonId);

    /**
     * Удаляет запись прогресса по связке пользователь-урок.
     */
    boolean deleteByUserIdAndLessonId(long userId, long lessonId);

    /**
     * Удаляет все записи прогресса пользователя.
     */
    int deleteByUserId(long userId);

    /**
     * Удаляет все записи прогресса урока.
     */
    int deleteByLessonId(long lessonId);

    /**
     * Возвращает все записи прогресса в системе.
     */
    List<LessonProgress> findAll();
}
