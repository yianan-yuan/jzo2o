package com.jzo2o.aigc;

import com.jzo2o.aigc.properties.AigcProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AigcProperties.class)
public class AigcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AigcApplication.class, args);
    }
}
