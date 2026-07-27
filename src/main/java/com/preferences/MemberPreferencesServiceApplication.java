package com.preferences;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MemberPreferencesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberPreferencesServiceApplication.class, args);
    }
}
