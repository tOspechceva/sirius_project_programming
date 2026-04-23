package digital.zil.hl.module1.repository.jpa;

import digital.zil.hl.module1.model.Lesson;
import digital.zil.hl.module1.model.LessonTest;
import digital.zil.hl.module1.repository.LessonRepository;
import digital.zil.hl.module1.repository.jpa.entity.LessonEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaLessonRepositoryAdapter implements LessonRepository {
    private final SpringDataLessonEntityRepository lessonEntityRepository;

    public JpaLessonRepositoryAdapter(final SpringDataLessonEntityRepository lessonEntityRepository) {
        this.lessonEntityRepository = lessonEntityRepository;
    }

    @Override
    @Transactional
    public Lesson create(
            final String topic,
            final int videoDurationMinutes,
            final String testName,
            final int maxTestScore
    ) {
        final LessonEntity entity = new LessonEntity();
        entity.setTopic(topic);
        entity.setVideoDurationMinutes(videoDurationMinutes);
        entity.setTestName(testName);
        entity.setMaxTestScore(maxTestScore);
        return toDomain(lessonEntityRepository.save(entity));
    }

    @Override
    public Optional<Lesson> findById(final long id) {
        return lessonEntityRepository.findById(id).map(JpaLessonRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<Lesson> update(
            final long id,
            final String topic,
            final int videoDurationMinutes,
            final String testName,
            final int maxTestScore
    ) {
        return lessonEntityRepository.findById(id)
                .map(entity -> {
                    entity.setTopic(topic);
                    entity.setVideoDurationMinutes(videoDurationMinutes);
                    entity.setTestName(testName);
                    entity.setMaxTestScore(maxTestScore);
                    return toDomain(lessonEntityRepository.save(entity));
                });
    }

    @Override
    @Transactional
    public boolean deleteById(final long id) {
        if (!lessonEntityRepository.existsById(id)) {
            return false;
        }
        lessonEntityRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Lesson> findAll() {
        return lessonEntityRepository.findAll().stream()
                .map(JpaLessonRepositoryAdapter::toDomain)
                .toList();
    }

    private static Lesson toDomain(final LessonEntity entity) {
        return new Lesson(
                entity.getId(),
                entity.getTopic(),
                entity.getVideoDurationMinutes(),
                new LessonTest(entity.getTestName(), entity.getMaxTestScore())
        );
    }
}
