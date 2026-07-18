package com.manager3d;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class Manager3dApplication {

    public static void main(String[] args) {
        SpringApplication.run(Manager3dApplication.class, args);
    }
}
