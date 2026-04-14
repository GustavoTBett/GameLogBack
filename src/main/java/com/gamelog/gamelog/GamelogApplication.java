package com.gamelog.gamelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GamelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamelogApplication.class, args);
	}

}
