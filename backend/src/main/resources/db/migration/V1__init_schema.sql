-- V1__init_schema.sql
-- Phase 0 core schema: multi-tenant foundation.
-- organization_id sits on every tenant-scoped table (users, courses, classrooms);
-- enrollments inherits tenant scoping indirectly through classroom_id.

CREATE TABLE organizations (
     id BIGSERIAL PRIMARY KEY,
     name VARCHAR(255) NOT NULL,
     slug VARCHAR(100) NOT NULL,
     created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

     CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE TABLE users (
     id BIGSERIAL PRIMARY KEY,
     organization_id BIGINT NOT NULL,
     name VARCHAR(255) NOT NULL,
     email VARCHAR(255) NOT NULL,
     password_hash   VARCHAR(255) NOT NULL,
     role VARCHAR(20) NOT NULL,   -- ADMIN | TEACHER | STUDENT
     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INVITED
     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

     CONSTRAINT fk_users_organization
     FOREIGN KEY (organization_id) REFERENCES organizations (id),
     CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_organization_id ON users (organization_id);

CREATE TABLE courses (
     id BIGSERIAL PRIMARY KEY,
     organization_id BIGINT NOT NULL,
     name VARCHAR(255) NOT NULL,
     created_by BIGINT NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

     CONSTRAINT fk_courses_organization
     FOREIGN KEY (organization_id) REFERENCES organizations (id),
     CONSTRAINT fk_courses_created_by
     FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_courses_organization_id ON courses (organization_id);

CREATE TABLE classrooms (
     id BIGSERIAL PRIMARY KEY,
     organization_id BIGINT NOT NULL,
     course_id BIGINT NOT NULL,
     name VARCHAR(255) NOT NULL,
     invite_code VARCHAR(20) NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

     CONSTRAINT fk_classrooms_organization
     FOREIGN KEY (organization_id) REFERENCES organizations (id),
     CONSTRAINT fk_classrooms_course
     FOREIGN KEY (course_id) REFERENCES courses (id),
     CONSTRAINT uq_classrooms_invite_code UNIQUE (invite_code)
);

CREATE INDEX idx_classrooms_organization_id ON classrooms (organization_id);
CREATE INDEX idx_classrooms_course_id ON classrooms (course_id);

CREATE TABLE enrollments (
     id BIGSERIAL PRIMARY KEY,
     organization_id BIGINT NOT NULL,
     classroom_id    BIGINT NOT NULL,
     student_id BIGINT NOT NULL,
     enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),

     CONSTRAINT fk_enrollments_organization
     FOREIGN KEY (organization_id) REFERENCES organizations (id),
     CONSTRAINT fk_enrollments_classroom
     FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE,
     CONSTRAINT fk_enrollments_student
     FOREIGN KEY (student_id) REFERENCES users (id),
    -- A student can't enroll in the same classroom twice
     CONSTRAINT uq_enrollments_classroom_student UNIQUE (classroom_id, student_id)
);

CREATE INDEX idx_enrollments_organization_id ON enrollments (organization_id);
CREATE INDEX idx_enrollments_student_id ON enrollments (student_id);