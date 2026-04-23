package digital.zil.hl.module1.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_progress_user_lesson", columnNames = {"user_id", "lesson_id"})
        }
)
public class LessonProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private LessonEntity lesson;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(name = "test_result", nullable = false)
    private int testResult;

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

    public LessonEntity getLesson() {
        return lesson;
    }

    public void setLesson(final LessonEntity lesson) {
        this.lesson = lesson;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(final LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public int getTestResult() {
        return testResult;
    }

    public void setTestResult(final int testResult) {
        this.testResult = testResult;
    }
}
