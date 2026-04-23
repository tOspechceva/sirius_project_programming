CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    registration_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    video_duration_minutes INTEGER NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    max_test_score INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    lesson_id BIGINT NOT NULL REFERENCES lessons (id),
    completion_date DATE NOT NULL,
    test_result INTEGER NOT NULL,
    CONSTRAINT uq_progress_user_lesson UNIQUE (user_id, lesson_id)
);
