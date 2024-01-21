package com.preservinc.production.djr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DailyJobReportsSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyJobReportsSpringApplication.class, args);
    }

}

// todo post initialization, create root user with default password if not exists