package digital.zil.hl.module1.api;

import digital.zil.hl.module1.api.dto.CompleteLessonRequest;
import digital.zil.hl.module1.api.dto.LessonProgressResponse;
import digital.zil.hl.module1.api.dto.UpdateLessonProgressRequest;
import digital.zil.hl.module1.api.dto.UserProgressResponse;
import digital.zil.hl.module1.entity.UserEntity;
import digital.zil.hl.module1.model.LessonProgress;
import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.UserRepository;
import digital.zil.hl.module1.service.CourseProgressService;
import digital.zil.hl.module1.service.ObservabilityService;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API для отметки прохождения уроков и получения прогресса.
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    private final CourseProgressService courseProgressService;
    private final UserRepository userRepository;
    private final ObservabilityService observabilityService;

    public ProgressController(
            final CourseProgressService courseProgressService,
            final UserRepository userRepository,
            final ObservabilityService observabilityService
    ) {
        this.courseProgressService = courseProgressService;
        this.userRepository = userRepository;
        this.observabilityService = observabilityService;
    }

    @PostMapping
    public ResponseEntity<LessonProgressResponse> createOrReplaceProgress(
            @RequestBody final CompleteLessonRequest request
    ) {
        return timed("controller:createOrReplaceProgress", () -> {
            final LessonProgress saved = courseProgressService.markLessonCompleted(
                    request.userId(),
                    request.lessonId(),
                    request.completionDate(),
                    request.testResult()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(toProgressResponse(saved));
        });
    }

    @PostMapping("/complete")
    public LessonProgressResponse completeLesson(@RequestBody final CompleteLessonRequest request) {
        return timed("controller:completeLesson", () -> {
            final LessonProgress saved = courseProgressService.markLessonCompleted(
                    request.userId(),
                    request.lessonId(),
                    request.completionDate(),
                    request.testResult()
            );
            return toProgressResponse(saved);
        });
    }

    @GetMapping
    public List<LessonProgressResponse> getAllProgressEntries() {
        return timed("controller:getAllProgressEntries", () -> courseProgressService.getAllProgressEntries().stream()
                .map(ProgressController::toProgressResponse)
                .toList());
    }

    @GetMapping("/users/{userId}/lessons")
    public List<LessonProgressResponse> getUserProgressEntries(@PathVariable final long userId) {
        return timed("controller:getUserProgressEntries", () -> courseProgressService.getProgressEntriesByUser(userId).stream()
                .map(ProgressController::toProgressResponse)
                .toList());
    }

    @GetMapping("/users/{userId}/lessons/{lessonId}")
    public LessonProgressResponse getProgressEntry(
            @PathVariable final long userId,
            @PathVariable final long lessonId
    ) {
        return timed("controller:getProgressEntry", () -> {
            final LessonProgress progress = courseProgressService.getProgressEntry(userId, lessonId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Прогресс не найден для userId=" + userId + ", lessonId=" + lessonId
                    ));
            return toProgressResponse(progress);
        });
    }

    @PutMapping("/users/{userId}/lessons/{lessonId}")
    public LessonProgressResponse updateProgressEntry(
            @PathVariable final long userId,
            @PathVariable final long lessonId,
            @RequestBody final UpdateLessonProgressRequest request
    ) {
        return timed("controller:updateProgressEntry", () -> {
            final LessonProgress updated = courseProgressService.markLessonCompleted(
                    userId,
                    lessonId,
                    request.completionDate(),
                    request.testResult()
            );
            return toProgressResponse(updated);
        });
    }

    @DeleteMapping("/users/{userId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteProgressEntry(
            @PathVariable final long userId,
            @PathVariable final long lessonId
    ) {
        return timed("controller:deleteProgressEntry", () -> {
            courseProgressService.deleteProgressEntry(userId, lessonId);
            return ResponseEntity.noContent().build();
        });
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearProgress() {
        return timed("controller:clearProgress", () -> {
            courseProgressService.deleteAllProgress();
            return ResponseEntity.noContent().build();
        });
    }

    @GetMapping("/users/{userId}")
    public UserProgressResponse getUserProgress(@PathVariable final long userId) {
        return timed("controller:getUserProgress", () -> {
            final User user = userRepository.findById(userId)
                    .map(ProgressController::toDomain)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
            final double progressPercent = courseProgressService.calculateUserProgressPercent(userId);
            return toUserProgressResponse(user, progressPercent);
        });
    }

    @GetMapping("/users")
    public List<UserProgressResponse> getAllUsersProgress() {
        return timed("controller:getAllUsersProgress", () -> {
            final Map<User, Double> allProgress = courseProgressService.calculateAllUsersProgressPercent();
            return allProgress.entrySet().stream()
                    .map(entry -> toUserProgressResponse(entry.getKey(), entry.getValue()))
                    .toList();
        });
    }

    private <T> T timed(final String operation, final Supplier<T> supplier) {
        final long started = System.nanoTime();
        try {
            final T result = supplier.get();
            observabilityService.recordSuccess(operation, System.nanoTime() - started);
            return result;
        } catch (RuntimeException ex) {
            observabilityService.recordFailure(operation, System.nanoTime() - started);
            throw ex;
        }
    }

    private static UserProgressResponse toUserProgressResponse(final User user, final double progressPercent) {
        return new UserProgressResponse(user.id(), user.login(), user.email(), progressPercent);
    }

    private static User toDomain(final UserEntity entity) {
        return new User(entity.getId(), entity.getLogin(), entity.getEmail(), entity.getRegistrationDate());
    }

    private static LessonProgressResponse toProgressResponse(final LessonProgress progress) {
        return new LessonProgressResponse(
                progress.userId(),
                progress.lessonId(),
                progress.completionDate(),
                progress.testResult()
        );
    }
}
