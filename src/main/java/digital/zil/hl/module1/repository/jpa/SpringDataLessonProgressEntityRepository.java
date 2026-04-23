package digital.zil.hl.module1.repository.jpa;

import digital.zil.hl.module1.repository.jpa.entity.LessonProgressEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLessonProgressEntityRepository extends JpaRepository<LessonProgressEntity, Long> {
    Optional<LessonProgressEntity> findByUser_IdAndLesson_Id(Long userId, Long lessonId);

    List<LessonProgressEntity> findByUser_Id(Long userId);

    List<LessonProgressEntity> findByLesson_Id(Long lessonId);
}
