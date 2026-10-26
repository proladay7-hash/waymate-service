package com.waymate.service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration providing a configured ObjectMapper bean for JSON parsing.
 */
@Configuration
public class JacksonConfig {

	@Bean
	@Primary
	@ConditionalOnMissingBean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.registerModule(new JavaTimeModule());
			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		} catch (Throwable ignored) {
			// Gracefully fallback if Jsr310 module is not loaded
		}
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}
}
