package com.example.students;
import jakarta.validation.constraints.*;
public record StudentRequest(
 @NotBlank @Size(max = 100) String name,
 @NotBlank @Email @Size(max = 254) String email,
 @NotBlank @Size(max = 100) String course,
 @NotNull @Min(1) @Max(150) Integer age
) {}
