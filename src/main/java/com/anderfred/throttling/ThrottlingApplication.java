package com.anderfred.throttling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan("com.anderfred.throttling.config")
public class ThrottlingApplication {

  public static void main(String[] args) {
    SpringApplication.run(ThrottlingApplication.class, args);
  }
}
