package com.waymate.service;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
class WaymateServiceApplicationTests {

	@Autowired(required = false)
	private Driver driver;

	@Test
	void contextLoads() {
	}

	@Test
	void testNeo4jConnection() {
		assertNotNull(driver, "Neo4j Driver should be configured");
		driver.verifyConnectivity();
	}
}
