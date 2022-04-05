package com.anderfred.throttling.advice;

import com.anderfred.throttling.exceptions.RequestCountExceedsException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(annotations = {Controller.class})
public class RequestCountExceptionAdvice {
  @ResponseStatus(HttpStatus.BAD_GATEWAY)  // 502
  @ExceptionHandler(RequestCountExceedsException.class)
  public void handleException() {
  }
}
