package com.example.cuttingservice.web;

import com.example.cuttingservice.entity.PartAlreadyPlacedException;
import com.example.cuttingservice.entity.PartTypeMismatchException;
import com.example.cuttingservice.entity.TrayCapacityExceededException;
import com.example.cuttingservice.service.AnimalPartNotFoundException;
import com.example.cuttingservice.service.DuplicateAnimalPartException;
import com.example.cuttingservice.service.DuplicateTrayException;
import com.example.cuttingservice.service.TrayNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler
{
  @ExceptionHandler({AnimalPartNotFoundException.class, TrayNotFoundException.class})
  public ProblemDetail handleNotFound(RuntimeException exception)
  {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler({DuplicateAnimalPartException.class, DuplicateTrayException.class})
  public ProblemDetail handleDuplicate(RuntimeException exception)
  {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler({PartTypeMismatchException.class, TrayCapacityExceededException.class,
                     PartAlreadyPlacedException.class})
  public ProblemDetail handleTrayRule(RuntimeException exception)
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
