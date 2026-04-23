INSERT INTO users (id, login, email, registration_date)
VALUES
    (1, 'ivan_student', 'ivan@example.com', '2026-03-03'),
    (2, 'olga_student', 'olga@example.com', '2026-03-10');

INSERT INTO lessons (id, topic, video_duration_minutes, test_name, max_test_score)
VALUES
    (1, 'Java Basics', 45, 'Test po sintaksisu', 10),
    (2, 'OOP in Java', 55, 'Test po OOP', 20),
    (3, 'Spring Intro', 60, 'Test po Spring', 25);

INSERT INTO lesson_progress (id, user_id, lesson_id, completion_date, test_result)
VALUES
    (1, 1, 1, '2026-04-01', 9),
    (2, 1, 2, '2026-04-05', 16),
    (3, 2, 1, '2026-04-06', 8),
    (4, 2, 3, '2026-04-12', 20);

SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('lessons_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lessons));
SELECT setval('lesson_progress_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lesson_progress));
