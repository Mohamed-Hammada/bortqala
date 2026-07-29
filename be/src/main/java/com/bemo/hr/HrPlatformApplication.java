package com.bemo.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HrPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(HrPlatformApplication.class, args);
	}

}
