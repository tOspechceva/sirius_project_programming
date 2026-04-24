package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.entity.LessonProgressEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Репозиторий прогресса обучения.
 *
 * <p>Сущность прогресса является связью многие-ко-многим между пользователем и уроком.
 */
public interface LessonProgressRepository extends CrudRepository<LessonProgressEntity, Long> {
    @Override
    List<LessonProgressEntity> findAll();

    Optional<LessonProgressEntity> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgressEntity> findByUserId(Long userId);

    List<LessonProgressEntity> findByLessonId(Long lessonId);

    long deleteByUserIdAndLessonId(Long userId, Long lessonId);

    long deleteByUserId(Long userId);

    long deleteByLessonId(Long lessonId);
}
