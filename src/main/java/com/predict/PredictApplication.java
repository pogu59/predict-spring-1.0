package com.predict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PredictApplication {

	public static void main(String[] args) {
		SpringApplication.run(PredictApplication.class, args);
	}

}
