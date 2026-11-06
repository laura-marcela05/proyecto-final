-- Demo data for local runs only. Fixed UUIDs, so the Postman collection can address every row.
-- Passwords are stored as bcrypt hashes (cost 10); the plain values are in the implementation manual.

INSERT INTO users (id, username, full_name, password_hash, role) VALUES
    ('00000000-0000-4000-8000-000000000001', 'admin',    'Pedro Ruiz',     '$2a$10$yQr/RnF6OzVuQC0EQ0FSF.Ps2vR6/FiUx0azeB8CmXEM1JPVSnLj6', 'ADMIN'),
    ('00000000-0000-4000-8000-000000000002', 'trainer1', 'Laura Gómez',    '$2a$10$LBNDmFGIgTcIPAqdUd6c5ekzixaONVwhA45i22OQYuyumauI1YGNa', 'TRAINER'),
    ('00000000-0000-4000-8000-000000000003', 'trainer2', 'Mateo Salazar',  '$2a$10$LBNDmFGIgTcIPAqdUd6c5ekzixaONVwhA45i22OQYuyumauI1YGNa', 'TRAINER');

INSERT INTO students (id, trainer_id, name, document) VALUES
    ('00000000-0000-4000-8000-000000000011', '00000000-0000-4000-8000-000000000002', 'Ana Torres',     '1082345678'),
    ('00000000-0000-4000-8000-000000000012', '00000000-0000-4000-8000-000000000002', 'Carlos Pérez',   '1019876543'),
    ('00000000-0000-4000-8000-000000000013', '00000000-0000-4000-8000-000000000003', 'Sofía Ramírez',  '1075123456');

-- One closed session of Ana Torres: 65 minutes, chest and arms.
INSERT INTO sessions (id, student_id, check_in_at, check_out_at) VALUES
    ('00000000-0000-4000-8000-000000000021', '00000000-0000-4000-8000-000000000011',
     '2026-09-24T23:00:00Z', '2026-09-25T00:05:00Z');

INSERT INTO session_muscle_groups (id, session_id, muscle_group, created_at) VALUES
    ('00000000-0000-4000-8000-000000000031', '00000000-0000-4000-8000-000000000021', 'chest', '2026-09-24T23:10:00Z'),
    ('00000000-0000-4000-8000-000000000032', '00000000-0000-4000-8000-000000000021', 'arms',  '2026-09-24T23:20:00Z');
