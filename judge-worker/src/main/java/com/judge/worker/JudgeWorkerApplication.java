package com.judge.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.judge.worker.model")
@EnableJpaRepositories(basePackages = "com.judge.worker.repository")
public class JudgeWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JudgeWorkerApplication.class, args);
    }
}