package com.datalog.watchlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WatchlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatchlogApplication.class, args);
	}

}
