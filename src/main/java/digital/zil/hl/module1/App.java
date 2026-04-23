package digital.zil.hl.module1;

import digital.zil.hl.module1.model.Lesson;
import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.LessonProgressRepository;
import digital.zil.hl.module1.repository.LessonRepository;
import digital.zil.hl.module1.repository.UserRepository;
import digital.zil.hl.module1.repository.inmemory.StaticLessonProgressRepository;
import digital.zil.hl.module1.repository.inmemory.StaticLessonRepository;
import digital.zil.hl.module1.repository.inmemory.StaticUserRepository;
import digital.zil.hl.module1.service.CompletionProgressCalculator;
import digital.zil.hl.module1.service.CourseProgressService;
import java.time.LocalDate;
import java.util.Map;

/**
 * Демонстрационный запуск приложения.
 *
 * <p>Показывает:
 * <ul>
 *     <li>создание пользователей и уроков;</li>
 *     <li>регистрацию прогресса прохождения;</li>
 *     <li>вывод прогресса одного пользователя;</li>
 *     <li>вывод прогресса всех пользователей.</li>
 * </ul>
 */
public final class App {
    private App() {
    }

    public static void main(final String[] args) {
        // Подключаем in-memory репозитории на static-коллекциях.
        final UserRepository userRepository = new StaticUserRepository();
        final LessonRepository lessonRepository = new StaticLessonRepository();
        final LessonProgressRepository lessonProgressRepository = new StaticLessonProgressRepository();

        // Прогресс вычисляется стратегией, которую можно заменить другой реализацией.
        final CourseProgressService progressService = new CourseProgressService(
                userRepository,
                lessonRepository,
                lessonProgressRepository,
                new CompletionProgressCalculator()
        );

        // 1) Создание учеников.
        final User ivan = userRepository.create("ivan_student", "ivan@example.com", LocalDate.of(2026, 3, 3));
        final User olga = userRepository.create("olga_student", "olga@example.com", LocalDate.of(2026, 3, 10));

        // 2) Создание уроков.
        final Lesson javaBasics = lessonRepository.create("Java Basics", 45, "Тест по синтаксису", 10);
        final Lesson oop = lessonRepository.create("ООП в Java", 55, "Тест по ООП", 20);
        final Lesson springIntro = lessonRepository.create("Spring Intro", 65, "Тест по Spring", 25);

        // 3) Фиксация завершения уроков и результатов тестов.
        progressService.markLessonCompleted(ivan.id(), javaBasics.id(), LocalDate.of(2026, 4, 1), 9);
        progressService.markLessonCompleted(ivan.id(), oop.id(), LocalDate.of(2026, 4, 5), 16);
        progressService.markLessonCompleted(olga.id(), javaBasics.id(), LocalDate.of(2026, 4, 6), 8);
        progressService.markLessonCompleted(olga.id(), springIntro.id(), LocalDate.of(2026, 4, 12), 20);

        // 4) Вывод прогресса одного выбранного ученика.
        final double oneStudentProgress = progressService.calculateUserProgressPercent(ivan.id());
        System.out.printf("Прогресс ученика %s: %.2f%%%n", ivan.login(), oneStudentProgress);

        // 5) Вывод прогресса всех учеников.
        System.out.println("Прогресс всех учеников:");
        final Map<User, Double> allProgress = progressService.calculateAllUsersProgressPercent();
        allProgress.forEach((user, percent) ->
                System.out.printf("- %s (%s): %.2f%%%n", user.login(), user.email(), percent)
        );
    }
}
