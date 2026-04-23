package digital.zil.hl.module1.repository.jpa;

import digital.zil.hl.module1.model.LessonProgress;
import digital.zil.hl.module1.repository.LessonProgressRepository;
import digital.zil.hl.module1.repository.jpa.entity.LessonEntity;
import digital.zil.hl.module1.repository.jpa.entity.LessonProgressEntity;
import digital.zil.hl.module1.repository.jpa.entity.UserEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaLessonProgressRepositoryAdapter implements LessonProgressRepository {
    private final SpringDataLessonProgressEntityRepository lessonProgressEntityRepository;
    private final SpringDataUserEntityRepository userEntityRepository;
    private final SpringDataLessonEntityRepository lessonEntityRepository;

    public JpaLessonProgressRepositoryAdapter(
            final SpringDataLessonProgressEntityRepository lessonProgressEntityRepository,
            final SpringDataUserEntityRepository userEntityRepository,
            final SpringDataLessonEntityRepository lessonEntityRepository
    ) {
        this.lessonProgressEntityRepository = lessonProgressEntityRepository;
        this.userEntityRepository = userEntityRepository;
        this.lessonEntityRepository = lessonEntityRepository;
    }

    @Override
    @Transactional
    public LessonProgress completeLesson(
            final long userId,
            final long lessonId,
            final LocalDate completionDate,
            final int testResult
    ) {
        final LessonProgressEntity entity = lessonProgressEntityRepository.findByUser_IdAndLesson_Id(userId, lessonId)
                .orElseGet(() -> createEntity(userId, lessonId));

        entity.setCompletionDate(completionDate);
        entity.setTestResult(testResult);
        return toDomain(lessonProgressEntityRepository.save(entity));
    }

    @Override
    public Optional<LessonProgress> findByUserIdAndLessonId(final long userId, final long lessonId) {
        return lessonProgressEntityRepository.findByUser_IdAndLesson_Id(userId, lessonId)
                .map(JpaLessonProgressRepositoryAdapter::toDomain);
    }

    @Override
    public List<LessonProgress> findByUserId(final long userId) {
        return lessonProgressEntityRepository.findByUser_Id(userId).stream()
                .map(JpaLessonProgressRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<LessonProgress> findByLessonId(final long lessonId) {
        return lessonProgressEntityRepository.findByLesson_Id(lessonId).stream()
                .map(JpaLessonProgressRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndLessonId(final long userId, final long lessonId) {
        final Optional<LessonProgressEntity> existing = lessonProgressEntityRepository.findByUser_IdAndLesson_Id(
                userId,
                lessonId
        );
        if (existing.isEmpty()) {
            return false;
        }
        lessonProgressEntityRepository.delete(existing.get());
        return true;
    }

    @Override
    @Transactional
    public int deleteByUserId(final long userId) {
        final List<LessonProgressEntity> entities = lessonProgressEntityRepository.findByUser_Id(userId);
        lessonProgressEntityRepository.deleteAll(entities);
        return entities.size();
    }

    @Override
    @Transactional
    public int deleteByLessonId(final long lessonId) {
        final List<LessonProgressEntity> entities = lessonProgressEntityRepository.findByLesson_Id(lessonId);
        lessonProgressEntityRepository.deleteAll(entities);
        return entities.size();
    }

    @Override
    public List<LessonProgress> findAll() {
        return lessonProgressEntityRepository.findAll().stream()
                .map(JpaLessonProgressRepositoryAdapter::toDomain)
                .toList();
    }

    private LessonProgressEntity createEntity(final long userId, final long lessonId) {
        final UserEntity user = userEntityRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
        final LessonEntity lesson = lessonEntityRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден: " + lessonId));

        final LessonProgressEntity entity = new LessonProgressEntity();
        entity.setUser(user);
        entity.setLesson(lesson);
        return entity;
    }

    private static LessonProgress toDomain(final LessonProgressEntity entity) {
        return new LessonProgress(
                entity.getUser().getId(),
                entity.getLesson().getId(),
                entity.getCompletionDate(),
                entity.getTestResult()
        );
    }
}
