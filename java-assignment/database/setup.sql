-- Run as a MySQL administrator for local development.
CREATE DATABASE IF NOT EXISTS student_management;
CREATE USER IF NOT EXISTS 'student_app'@'localhost' IDENTIFIED BY 'student_local_password';
GRANT ALL PRIVILEGES ON student_management.* TO 'student_app'@'localhost';
-- Hibernate creates the students table when the application starts.
