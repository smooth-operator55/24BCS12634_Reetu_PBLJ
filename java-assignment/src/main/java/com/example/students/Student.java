package com.example.students;
import jakarta.persistence.*;
@Entity
@Table(name = "students")
public class Student {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;
 @Column(nullable = false, length = 100) private String name;
 @Column(nullable = false, length = 254) private String email;
 @Column(nullable = false, length = 100) private String course;
 @Column(nullable = false) private Integer age;
 protected Student() {}
 public Student(StudentRequest request) { update(request); }
 public void update(StudentRequest request) {
  name = request.name().trim(); email = request.email().trim();
  course = request.course().trim(); age = request.age();
 }
 public Long getId() { return id; }
 public String getName() { return name; }
 public String getEmail() { return email; }
 public String getCourse() { return course; }
 public Integer getAge() { return age; }
}
