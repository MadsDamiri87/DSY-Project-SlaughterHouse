package com.example.cuttingservice.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class RestExceptionHandler
{

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception)
    {
      String details = exception.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + " " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

      return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
    }
}
