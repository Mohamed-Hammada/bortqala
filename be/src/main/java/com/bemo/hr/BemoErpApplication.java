package com.bemo.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BemoErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(BemoErpApplication.class, args);
	}

}
