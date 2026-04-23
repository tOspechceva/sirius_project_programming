package digital.zil.hl.module1.repository.jpa;

import digital.zil.hl.module1.repository.jpa.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLessonEntityRepository extends JpaRepository<LessonEntity, Long> {
}
