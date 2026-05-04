package com.clinicare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClinicareApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicareApplication.class, args);
    }
}