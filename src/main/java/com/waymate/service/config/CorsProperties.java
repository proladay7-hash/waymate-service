package com.waymate.service.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "waymate.cors")
public class CorsProperties {

	private List<String> allowedOriginPatterns = List.of("*");
}
