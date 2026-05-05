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
@Service  // Делает класс управляемым Spring-контейнером (автосканирование, внедрение зависимостей)
public class KafkaCommandProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaCommandProcessor.class);  // Логгер для вывода информации и ошибок

    // Зависимости, внедряемые через конструктор (лучшая практика: final + immutable)
    private final ObjectMapper objectMapper;  // JSON ↔ Java Object (десериализация сообщений)
    private final UserRepository userRepository;  // Репозиторий для работы с пользователями
    private final LessonRepository lessonRepository;  // Репозиторий для работы с уроками
    private final CourseProgressService courseProgressService;  // Сервис для логики прогресса обучения

    // Конструктор с явной инъекцией зависимостей (защита от null, удобно для тестов)
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
     * Точка входа: обработка сырого JSON из Kafka
     * Ошибки логируются, но не пробрасываются — чтобы потребитель не зацикливался на «битом» сообщении
     */
    public void handle(final String json) {  // Принимает тело сообщения как строку
        try {
            final KafkaCommandEnvelope envelope = objectMapper.readValue(json, KafkaCommandEnvelope.class);  // Парсит JSON в обёртку-команду
            dispatch(envelope);  // Делегирует обработку по типу сущности
        } catch (final Exception ex) {
            LOG.error("Kafka command failed, body={}", json, ex);  // Логирует ошибку с телом сообщения для отладки
        }
    }

    // Маршрутизатор: выбирает обработчик по типу сущности (USER/LESSON/PROGRESS)
    private void dispatch(final KafkaCommandEnvelope envelope) {
        switch (envelope.entity()) {  // Проверка типа сущности из команды
            case USER -> handleUser(envelope.operation(), envelope.payload());  // Передаёт на обработку пользователя
            case LESSON -> handleLesson(envelope.operation(), envelope.payload());  // Передаёт на обработку урока
            case PROGRESS -> handleProgress(envelope.operation(), envelope.payload());  // Передаёт на обработку прогресса
        }
    }

    // Обработчик команд для сущности User
    private void handleUser(final KafkaEventOperation operation, final JsonNode payload) {
        switch (operation) {  // Проверка операции: создание или удаление
            case POST -> createUser(payload);  // Создать пользователя
            case DEL -> deleteUser(payload);  // Удалить пользователя
        }
    }

    // Создание пользователя из JSON
    private void createUser(final JsonNode payload) {
        final CreateUserRequest request = objectMapper.convertValue(payload, CreateUserRequest.class);  // JSON → DTO
        final UserEntity entity = new UserEntity();  // Создание новой сущности БД
        entity.setLogin(request.login());  // Заполнение полей из DTO
        entity.setEmail(request.email());
        entity.setRegistrationDate(request.registrationDate());
        userRepository.save(entity);  // Сохранение в БД
        LOG.info("Kafka: created user login={}", request.login());  // Лог успеха
    }

    // Удаление пользователя по ID
    private void deleteUser(final JsonNode payload) {
        final long id = readRequiredLong(payload, "id");  // Извлечение обязательного поля "id"
        courseProgressService.deleteAllProgressForUser(id);  // Сначала удаляем связанный прогресс (чтобы не нарушить целостность)
        if (!userRepository.existsById(id)) {  // Проверка существования
            throw new IllegalArgumentException("Пользователь не найден: " + id);  // Ошибка, если не найден
        }
        userRepository.deleteById(id);  // Удаление из БД
        LOG.info("Kafka: deleted user id={}", id);  // Лог успеха
    }

    // Обработчик команд для сущности Lesson
    private void handleLesson(final KafkaEventOperation operation, final JsonNode payload) {
        switch (operation) {
            case POST -> createLesson(payload);  // Создать урок
            case DEL -> deleteLesson(payload);  // Удалить урок
        }
    }

    // Создание урока из JSON
    private void createLesson(final JsonNode payload) {
        final CreateLessonRequest request = objectMapper.convertValue(payload, CreateLessonRequest.class);  // JSON → DTO
        final LessonEntity entity = new LessonEntity();  // Новая сущность
        entity.setTopic(request.topic());  // Заполнение полей
        entity.setVideoDurationMinutes(request.videoDurationMinutes());
        entity.setTestName(request.testName());
        entity.setMaxTestScore(request.maxTestScore());
        lessonRepository.save(entity);  // Сохранение в БД
        LOG.info("Kafka: created lesson topic={}", request.topic());  // Лог успеха
    }

    // Удаление урока по ID
    private void deleteLesson(final JsonNode payload) {
        final long id = readRequiredLong(payload, "id");  // Извлечение ID
        courseProgressService.deleteAllProgressForLesson(id);  // Удаляем прогресс по уроку (каскадная логика)
        if (!lessonRepository.existsById(id)) {  // Проверка существования
            throw new IllegalArgumentException("Урок не найден: " + id);  // Ошибка
        }
        lessonRepository.deleteById(id);  // Удаление из БД
        LOG.info("Kafka: deleted lesson id={}", id);  // Лог успеха
    }

    // Обработчик команд для сущности Progress
    private void handleProgress(final KafkaEventOperation operation, final JsonNode payload) {
        switch (operation) {
            case POST -> upsertProgress(payload);  // Создать или обновить прогресс
            case DEL -> deleteProgress(payload);  // Удалить запись прогресса
        }
    }

    // Обновление/создание прогресса прохождения урока
    private void upsertProgress(final JsonNode payload) {
        final CompleteLessonRequest request = objectMapper.convertValue(payload, CompleteLessonRequest.class);  // JSON → DTO
        courseProgressService.markLessonCompleted(  // Делегирование бизнес-логики сервису
                request.userId(),
                request.lessonId(),
                request.completionDate(),
                request.testResult()
        );
        LOG.info("Kafka: upsert progress userId={} lessonId={}", request.userId(), request.lessonId());  // Лог
    }

    // Удаление записи о прогрессе
    private void deleteProgress(final JsonNode payload) {
        final long userId = readRequiredLong(payload, "userId");  // Извлечение userId
        final long lessonId = readRequiredLong(payload, "lessonId");  // Извлечение lessonId
        courseProgressService.deleteProgressEntry(userId, lessonId);  // Вызов сервиса удаления
        LOG.info("Kafka: deleted progress userId={} lessonId={}", userId, lessonId);  // Лог
    }

    // Вспомогательный метод: безопасное извлечение обязательного long-поля из JSON
    private static long readRequiredLong(final JsonNode payload, final String field) {
        if (payload == null || !payload.hasNonNull(field)) {  // Проверка: поле существует и не null?
            throw new IllegalArgumentException("payload." + field + " is required");  // Ошибка, если поле отсутствует
        }
        return payload.get(field).asLong();  // Преобразование в long и возврат
    }
}