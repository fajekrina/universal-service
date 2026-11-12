package com.fajekrina.universal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.fajekrina.universal.config.properties")
public class UniversalApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniversalApplication.class, args);
    }
}
