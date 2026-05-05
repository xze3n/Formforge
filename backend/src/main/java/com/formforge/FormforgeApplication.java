package com.formforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FormforgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormforgeApplication.class, args);
    }

}
