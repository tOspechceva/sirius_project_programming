package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.entity.LessonEntity;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Репозиторий уроков.
 */
public interface LessonRepository extends CrudRepository<LessonEntity, Long> {
    @Override
    List<LessonEntity> findAll();
}
