package digital.zil.hl.module1.api;

import digital.zil.hl.module1.api.dto.CreateLessonRequest;
import digital.zil.hl.module1.api.dto.LessonResponse;
import digital.zil.hl.module1.api.dto.UpdateLessonRequest;
import digital.zil.hl.module1.model.Lesson;
import digital.zil.hl.module1.repository.LessonRepository;
import digital.zil.hl.module1.service.CourseProgressService;
import java.util.List;
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
 * HTTP API для работы с уроками.
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonRepository lessonRepository;
    private final CourseProgressService courseProgressService;

    public LessonController(
            final LessonRepository lessonRepository,
            final CourseProgressService courseProgressService
    ) {
        this.lessonRepository = lessonRepository;
        this.courseProgressService = courseProgressService;
    }

    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@RequestBody final CreateLessonRequest request) {
        final Lesson created = lessonRepository.create(
                request.topic(),
                request.videoDurationMinutes(),
                request.testName(),
                request.maxTestScore()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toLessonResponse(created));
    }

    @GetMapping
    public List<LessonResponse> getAllLessons() {
        return lessonRepository.findAll().stream()
                .map(LessonController::toLessonResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public LessonResponse getLessonById(@PathVariable final long id) {
        final Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден: " + id));
        return toLessonResponse(lesson);
    }

    @PutMapping("/{id}")
    public LessonResponse updateLesson(
            @PathVariable final long id,
            @RequestBody final UpdateLessonRequest request
    ) {
        final Lesson updated = lessonRepository.update(
                        id,
                        request.topic(),
                        request.videoDurationMinutes(),
                        request.testName(),
                        request.maxTestScore()
                )
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден: " + id));
        return toLessonResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable final long id) {
        courseProgressService.deleteAllProgressForLesson(id);
        final boolean deleted = lessonRepository.deleteById(id);
        if (!deleted) {
            throw new IllegalArgumentException("Урок не найден: " + id);
        }
        return ResponseEntity.noContent().build();
    }

    private static LessonResponse toLessonResponse(final Lesson lesson) {
        return new LessonResponse(
                lesson.id(),
                lesson.topic(),
                lesson.videoDurationMinutes(),
                lesson.test().name(),
                lesson.test().maxScore()
        );
    }
}
