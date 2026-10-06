package com.example.registration.web;

import com.example.registration.service.AnimalNotFoundException;
import com.example.registration.service.DuplicateRegistrationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler
{
  @ExceptionHandler(AnimalNotFoundException.class)
  public ProblemDetail handleNotFound(AnimalNotFoundException exception)
  {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(DuplicateRegistrationException.class)
  public ProblemDetail handleDuplicate(DuplicateRegistrationException exception)
  {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException exception)
  {
    String details = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));

    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
  }
}
