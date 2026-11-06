-- Gym Track schema. Source: g-t-docs 06-data/models.md and database-conventions.md.
-- Runs once, when the database volume is empty (docker-entrypoint-initdb.d).

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'TRAINER'))
);

CREATE TABLE students (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    document   VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_students_document UNIQUE (document),
    CONSTRAINT fk_students_trainer FOREIGN KEY (trainer_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_students_trainer_id ON students (trainer_id);

CREATE TABLE sessions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID        NOT NULL,
    check_in_at  TIMESTAMPTZ NOT NULL,
    check_out_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sessions_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE RESTRICT,
    CONSTRAINT ck_sessions_check_out CHECK (check_out_at >= check_in_at)
);

CREATE INDEX idx_sessions_student_id ON sessions (student_id);

-- BR-07: at most one open session per student.
CREATE UNIQUE INDEX idx_sessions_student_id_open ON sessions (student_id) WHERE check_out_at IS NULL;

CREATE TABLE session_muscle_groups (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID        NOT NULL,
    muscle_group VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_session_muscle_groups_session FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE CASCADE,
    CONSTRAINT uq_session_muscle_groups UNIQUE (session_id, muscle_group),
    CONSTRAINT ck_session_muscle_groups_group CHECK (muscle_group IN ('chest', 'back', 'legs', 'arms', 'shoulders', 'core'))
);
