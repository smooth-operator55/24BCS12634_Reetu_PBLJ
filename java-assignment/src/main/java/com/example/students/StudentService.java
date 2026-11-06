package com.example.students;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class StudentService {
 private final StudentRepository repository;
 public StudentService(StudentRepository repository) { this.repository = repository; }
 public List<Student> all() { return repository.findAll(Sort.by("id")); }
 public Student get(Long id) { return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id)); }
 public List<Student> byCourse(String course) { return repository.findByCourseIgnoreCaseOrderByIdAsc(course.trim()); }
 @Transactional public Student create(StudentRequest request) { return repository.save(new Student(request)); }
 @Transactional public Student update(Long id, StudentRequest request) {
  Student student = get(id); student.update(request); return repository.save(student);
 }
 @Transactional public void delete(Long id) { repository.delete(get(id)); }
}
