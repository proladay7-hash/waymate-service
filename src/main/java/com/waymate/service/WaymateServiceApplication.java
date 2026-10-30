package com.waymate.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableNeo4jAuditing
@EnableScheduling
public class WaymateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WaymateServiceApplication.class, args);
	}

}