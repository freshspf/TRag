package com.spf.tbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TBackendApplication.class, args);
    }
}
