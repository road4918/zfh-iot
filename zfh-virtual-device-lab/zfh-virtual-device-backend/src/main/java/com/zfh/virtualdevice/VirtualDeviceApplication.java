package com.zfh.virtualdevice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VirtualDeviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VirtualDeviceApplication.class, args);
    }
}
