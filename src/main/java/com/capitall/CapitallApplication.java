package com.capitall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CapitallApplication {
    public static void main(String[] args) {
        SpringApplication.run(CapitallApplication.class, args);
    }
}
