package com.anderfred.throttling.controller;

import com.anderfred.throttling.annotations.ThrottleHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class ThrottlingController {
  @GetMapping("/test")
  @ThrottleHandler()
  ResponseEntity<Object> test() {
    return ResponseEntity.ok().build();
  }
}
