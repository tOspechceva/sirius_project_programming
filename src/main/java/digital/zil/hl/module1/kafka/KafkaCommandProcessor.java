package digital.zil.hl.module1.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import digital.zil.hl.module1.api.dto.CompleteLessonRequest;
import digital.zil.hl.module1.api.dto.CreateLessonRequest;
import digital.zil.hl.module1.api.dto.CreateUserRequest;
import digital.zil.hl.module1.entity.LessonEntity;
import digital.zil.hl.module1.entity.UserEntity;
import digital.zil.hl.module1.repository.LessonRepository;
import digital.zil.hl.module1.repository.UserRepository;
import digital.zil.hl.module1.service.CourseProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Разбор JSON-команд из Kafka и вызов соответствующих сервисов/репозиториев.
 */
@Service
public class KafkaCommandProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaCommandProcessor.class);

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final CourseProgressService courseProgressService;

    public KafkaCommandProcessor(
            final ObjectMapper objectMapper,
            final UserRepository userRepository,
            final LessonRepository lessonRepository,
            final CourseProgressService courseProgressService
    ) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.courseProgressService = courseProgressService;
    }

    /**
     * Обрабатывает сырое тело сообщения. При ошибке логирует и не пробрасывает
     * (чтобы не зациклить consumer на «ядовом» сообщении в учебном сценарии).
     */
    public void handle(final String json) {
        try {
            final KafkaCommandEnvelope envelope = objectMapper.readValue(json, KafkaCommandEnvelope.class);
            dispatch(envelope);
        } catch (final Exception ex) {
            LOG.error("Kafka command failed, body={}", json, ex);
        }
    }

    private void dispatch(final KafkaCommandEnvelope envelope) {
        switch (envelope.entity()) {
            case USER -> handleUser(envelope.operation(), envelope.payload());
            case LESSON -> handleLesson(envelope.operation(), envelope.payload());
            case PROGRESS -> handleProgress(envelope.operation(), envelope.payload());
        }
    }

    private void handleUser(final KafkaEventOperation operation, final JsonNode payload) {
        switch (operation) {
            case POST -> createUser(payload);
            case DEL -> deleteUser(payload);
        }
    }

    private void createUser(final JsonNode payload) {
        final CreateUserRequest request = objectMapper.convertValue(payload, CreateUserRequest.class);
        final UserEntity entity = new UserEntity();
        entity.setLogin(request.login());
        entity.setEmail(request.email());
        entity.setRegistrationDate(request.registrationDate());
        userRepository.save(entity);
        LOG.info("Kafka: created user login={}", request.login());
    }

    private void deleteUser(final JsonNode payload) {
        final long id = readRequiredLong(payload, "id");
        courseProgressService.deleteAllProgressForUser(id);
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Пользователь не найден: " + id);
        }
        userRepository.deleteById(id);
        LOG.info("Kafka: deleted user id={}", id);
    }

    private void handleLesson(final KafkaEventOperation operation, final JsonNode payload) {
        switch (operation) {
            case POST -> createLesson(payload);
            case DEL -> deleteLesson(payload);
        }
    }

    private void createLesson(final JsonNode payload) {
        final CreateLessonRequest request = objectMapper.convertValue(payload, CreateLessonRequest.class);
        final LessonEntity entity = new LessonEntity();
        entity.setTopic(request.topic());
        entity.setVideoDurationMinutes(request.videoDurationMinutes());
        entity.setTestName(request.testName());
        entity.setMaxTestScore(request.maxTestScore());
        lessonRepository.save(entity);
        LOG.info("Kafka: created lesson topic={}", request.topic());
    }

    private void deleteLesson(final JsonNode payload) {
        final long id = readRequiredLong(payload, "id");
        courseProgressService.deleteAllProgressForLesson(id);
        if (!lessonRepository.existsById(id)) {
            throw new IllegalArgumentException("Урок не найден: " + id);
        }
        lessonRepository.deleteById(id);
        LOG.info("Kafka: deleted lesson id={}", id);
    }

    private void handleProgress(final KafkaEventOperation operation, final JsonNode payload) {
        switch (operation) {
            case POST -> upsertProgress(payload);
            case DEL -> deleteProgress(payload);
        }
    }

    private void upsertProgress(final JsonNode payload) {
        final CompleteLessonRequest request = objectMapper.convertValue(payload, CompleteLessonRequest.class);
        courseProgressService.markLessonCompleted(
                request.userId(),
                request.lessonId(),
                request.completionDate(),
                request.testResult()
        );
        LOG.info("Kafka: upsert progress userId={} lessonId={}", request.userId(), request.lessonId());
    }

    private void deleteProgress(final JsonNode payload) {
        final long userId = readRequiredLong(payload, "userId");
        final long lessonId = readRequiredLong(payload, "lessonId");
        courseProgressService.deleteProgressEntry(userId, lessonId);
        LOG.info("Kafka: deleted progress userId={} lessonId={}", userId, lessonId);
    }

    private static long readRequiredLong(final JsonNode payload, final String field) {
        if (payload == null || !payload.hasNonNull(field)) {
            throw new IllegalArgumentException("payload." + field + " is required");
        }
        return payload.get(field).asLong();
    }
}
