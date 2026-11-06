package com.example.students;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/students")
public class StudentController {
 private final StudentService service;
 public StudentController(StudentService service) { this.service = service; }
 @PostMapping public ResponseEntity<Student> create(@Valid @RequestBody StudentRequest request) {
  Student student = service.create(request);
  return ResponseEntity.created(URI.create("/api/students/" + student.getId())).body(student);
 }
 @GetMapping public List<Student> all() { return service.all(); }
 @GetMapping("/{id}") public Student get(@PathVariable Long id) { return service.get(id); }
 @PutMapping("/{id}") public Student update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) { return service.update(id, request); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
 @GetMapping("/course/{course}") public List<Student> byCourse(@PathVariable String course) { return service.byCourse(course); }
}
