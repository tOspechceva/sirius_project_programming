package digital.zil.hl.module1.service;

import digital.zil.hl.module1.entity.LessonEntity;
import digital.zil.hl.module1.entity.LessonProgressEntity;
import digital.zil.hl.module1.entity.UserEntity;
import digital.zil.hl.module1.model.LessonProgress;
import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.LessonProgressRepository;
import digital.zil.hl.module1.repository.LessonRepository;
import digital.zil.hl.module1.repository.UserRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Центральный сервис бизнес-логики платформы онлайн-курсов.
 *
 * <p>Отвечает за:
 * <ul>
 *     <li>регистрацию факта завершения урока пользователем;</li>
 *     <li>валидацию даты завершения и результата теста;</li>
 *     <li>расчет прогресса конкретного пользователя;</li>
 *     <li>расчет прогресса по всем пользователям.</li>
 * </ul>
 *
 * <p>Сервис построен через интерфейсы репозиториев и стратегию расчета
 * ({@link ProgressCalculator}), поэтому логику хранения и формулу прогресса
 * можно менять без изменения этого класса.
 */
public final class CourseProgressService {
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ProgressCalculator progressCalculator;

    public CourseProgressService(
            final UserRepository userRepository,
            final LessonRepository lessonRepository,
            final LessonProgressRepository lessonProgressRepository,
            final ProgressCalculator progressCalculator
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.lessonRepository = Objects.requireNonNull(lessonRepository);
        this.lessonProgressRepository = Objects.requireNonNull(lessonProgressRepository);
        this.progressCalculator = Objects.requireNonNull(progressCalculator);
    }

    /**
     * Отмечает, что пользователь завершил конкретный урок.
     *
     * <p>Перед сохранением проверяет:
     * <ul>
     *     <li>существование пользователя;</li>
     *     <li>существование урока;</li>
     *     <li>корректность даты завершения;</li>
     *     <li>корректность результата теста в границах урока.</li>
     * </ul>
     *
     * @param userId идентификатор пользователя
     * @param lessonId идентификатор урока
     * @param completionDate дата завершения урока
     * @param testResult полученный балл по тесту
     * @return сохраненная запись прогресса (связь пользователь-урок)
     */
    public LessonProgress markLessonCompleted(
            final long userId,
            final long lessonId,
            final LocalDate completionDate,
            final int testResult
    ) {
        final UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
        final LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден: " + lessonId));

        validateCompletionData(user, lesson, completionDate, testResult);
        final LessonProgressEntity progressEntity = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    final LessonProgressEntity created = new LessonProgressEntity();
                    created.setUserId(userId);
                    created.setLessonId(lessonId);
                    return created;
                });
        progressEntity.setCompletionDate(completionDate);
        progressEntity.setTestResult(testResult);
        final LessonProgressEntity saved = lessonProgressRepository.save(progressEntity);
        return toDomain(saved);
    }

    /**
     * Возвращает одну запись прогресса по связке пользователь-урок.
     */
    public Optional<LessonProgress> getProgressEntry(final long userId, final long lessonId) {
        return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId).map(CourseProgressService::toDomain);
    }

    /**
     * Возвращает все записи прогресса конкретного пользователя.
     */
    public List<LessonProgress> getProgressEntriesByUser(final long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
        return lessonProgressRepository.findByUserId(userId).stream()
                .map(CourseProgressService::toDomain)
                .toList();
    }

    /**
     * Возвращает все записи прогресса в системе.
     */
    public List<LessonProgress> getAllProgressEntries() {
        return lessonProgressRepository.findAll().stream()
                .map(CourseProgressService::toDomain)
                .toList();
    }

    /**
     * Удаляет запись прогресса по связке пользователь-урок.
     */
    public void deleteProgressEntry(final long userId, final long lessonId) {
        final long deleted = lessonProgressRepository.deleteByUserIdAndLessonId(userId, lessonId);
        if (deleted == 0) {
            throw new IllegalArgumentException(
                    "Прогресс не найден для userId=" + userId + ", lessonId=" + lessonId
            );
        }
    }

    /**
     * Каскадно удаляет весь прогресс пользователя.
     */
    public int deleteAllProgressForUser(final long userId) {
        return (int) lessonProgressRepository.deleteByUserId(userId);
    }

    /**
     * Каскадно удаляет весь прогресс урока.
     */
    public int deleteAllProgressForLesson(final long lessonId) {
        return (int) lessonProgressRepository.deleteByLessonId(lessonId);
    }

    /**
     * Рассчитывает прогресс одного пользователя в процентах.
     *
     * <p>Формула делегируется объекту {@link ProgressCalculator},
     * что позволяет гибко менять методику оценки прогресса.
     *
     * @param userId идентификатор пользователя
     * @return прогресс в процентах (0..100)
     */
    public double calculateUserProgressPercent(final long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        final List<LessonEntity> allLessons = lessonRepository.findAll();
        final List<LessonProgress> userProgress = lessonProgressRepository.findByUserId(userId).stream()
                .map(CourseProgressService::toDomain)
                .toList();
        return progressCalculator.calculatePercent(allLessons.size(), userProgress);
    }

    /**
     * Рассчитывает прогресс для всех пользователей системы.
     *
     * @return карта "пользователь -> прогресс в процентах"
     */
    public Map<User, Double> calculateAllUsersProgressPercent() {
        final List<UserEntity> users = userRepository.findAll();
        final int allLessonsCount = lessonRepository.findAll().size();

        final Map<User, Double> progressByUser = new LinkedHashMap<>();
        for (final UserEntity user : users) {
            final List<LessonProgress> userProgress = lessonProgressRepository.findByUserId(user.getId()).stream()
                    .map(CourseProgressService::toDomain)
                    .toList();
            final double progressPercent = progressCalculator.calculatePercent(allLessonsCount, userProgress);
            progressByUser.put(toDomain(user), progressPercent);
        }

        return progressByUser;
    }

    /**
     * Внутренняя валидация данных завершения урока.
     *
     * <p>Набор правил можно расширять:
     * например, добавлять проверку дедлайнов, проверки попыток теста
     * или учет минимального проходного балла.
     */
    private static LessonProgress toDomain(final LessonProgressEntity entity) {
        return new LessonProgress(
                entity.getUserId(),
                entity.getLessonId(),
                entity.getCompletionDate(),
                entity.getTestResult()
        );
    }

    private static User toDomain(final UserEntity entity) {
        return new User(entity.getId(), entity.getLogin(), entity.getEmail(), entity.getRegistrationDate());
    }

    private static void validateCompletionData(
            final UserEntity user,
            final LessonEntity lesson,
            final LocalDate completionDate,
            final int testResult
    ) {
        if (completionDate == null) {
            throw new IllegalArgumentException("Дата завершения не может быть пустой");
        }
        if (completionDate.isBefore(user.getRegistrationDate())) {
            throw new IllegalArgumentException("Дата завершения не может быть раньше даты регистрации пользователя");
        }
        if (testResult < 0 || testResult > lesson.getMaxTestScore()) {
            throw new IllegalArgumentException(
                    "Результат теста должен быть в диапазоне от 0 до " + lesson.getMaxTestScore()
            );
        }
    }
}
