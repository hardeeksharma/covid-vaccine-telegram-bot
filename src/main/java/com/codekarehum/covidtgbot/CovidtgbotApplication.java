package com.codekarehum.covidtgbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@EnableScheduling
@SpringBootApplication
public class CovidtgbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(CovidtgbotApplication.class, args);
	}
	@Bean
	RestTemplate getRestTemplate(){
		return new RestTemplate();
	}
}
