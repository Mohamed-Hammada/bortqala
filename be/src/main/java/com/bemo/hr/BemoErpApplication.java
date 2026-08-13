package com.bemo.hr;

import com.bemo.hr.shared.nativeimage.LiquibaseRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@ImportRuntimeHints(LiquibaseRuntimeHints.class)
public class BemoErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(BemoErpApplication.class, args);
	}

}
