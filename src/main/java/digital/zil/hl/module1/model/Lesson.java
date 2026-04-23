package digital.zil.hl.module1.model;

/**
 * Урок в рамках курса.
 *
 * @param id идентификатор урока
 * @param topic тема урока
 * @param videoDurationMinutes длительность видео в минутах
 * @param test тест к уроку
 */
public record Lesson(long id, String topic, int videoDurationMinutes, LessonTest test) {
}
