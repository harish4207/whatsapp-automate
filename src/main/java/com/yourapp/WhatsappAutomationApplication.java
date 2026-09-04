package com.yourapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhatsappAutomationApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatsappAutomationApplication.class, args);
	}

}
