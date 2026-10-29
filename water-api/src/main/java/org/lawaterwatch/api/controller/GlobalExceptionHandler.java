package org.lawaterwatch.api.controller;

import org.lawaterwatch.api.service.PwsidNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PwsidNotFoundException.class)
    public ProblemDetail handlePwsidNotFound(PwsidNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Pwsid Not Found");
        return problem;
    }
}
