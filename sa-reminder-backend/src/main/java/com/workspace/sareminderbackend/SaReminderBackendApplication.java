package com.workspace.sareminderbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.workspace.sareminderbackend.mapper")
public class SaReminderBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaReminderBackendApplication.class, args);
    }

}
