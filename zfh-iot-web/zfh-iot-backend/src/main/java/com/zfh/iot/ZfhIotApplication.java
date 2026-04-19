package com.zfh.iot;

import org.apache.shiro.spring.boot.autoconfigure.ShiroAutoConfiguration;
import org.apache.shiro.spring.config.web.autoconfigure.ShiroWebAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {
    ShiroAutoConfiguration.class,
    ShiroWebAutoConfiguration.class
})
public class ZfhIotApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZfhIotApplication.class, args);
    }
}
