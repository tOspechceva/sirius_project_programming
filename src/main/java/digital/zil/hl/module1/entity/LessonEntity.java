package digital.zil.hl.module1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lessons")
public class LessonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "video_duration_minutes", nullable = false)
    private int videoDurationMinutes;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "max_test_score", nullable = false)
    private int maxTestScore;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(final String topic) {
        this.topic = topic;
    }

    public int getVideoDurationMinutes() {
        return videoDurationMinutes;
    }

    public void setVideoDurationMinutes(final int videoDurationMinutes) {
        this.videoDurationMinutes = videoDurationMinutes;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public int getMaxTestScore() {
        return maxTestScore;
    }

    public void setMaxTestScore(final int maxTestScore) {
        this.maxTestScore = maxTestScore;
    }
}
