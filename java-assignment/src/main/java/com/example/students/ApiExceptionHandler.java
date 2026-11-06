package com.example.students;
import java.util.TreeMap;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
 @ExceptionHandler(StudentNotFoundException.class)
 public ProblemDetail notFound(StudentNotFoundException exception) {
  return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
 }
 @Override
 protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
  HttpHeaders headers, HttpStatusCode status, org.springframework.web.context.request.WebRequest request) {
  var errors = new TreeMap<String, String>();
  ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
  var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Please correct the invalid fields");
  problem.setProperty("errors", errors);
  return handleExceptionInternal(ex, problem, headers, status, request);
 }
}
