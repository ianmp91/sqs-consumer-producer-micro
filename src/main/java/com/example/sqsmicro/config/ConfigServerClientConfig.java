package com.example.sqsmicro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @author ian.paris
 * @since 2026-01-14
 */
@Configuration
public class ConfigServerClientConfig {

	@Value("${config.server.url:http://localhost:8888}")
	private String configServerUrl;

	@Value("${app.client.id:airport-c}")
	private String clientId;

	@Value("${app.client.secret:secret-c-456}") // En prod, viene de ENV VARS
	private String clientSecret;

	@Bean("configRestClient")
	public RestClient configRestClient() {
		return RestClient.builder()
				.baseUrl(configServerUrl)
				.defaultHeader("X-Client-Id", clientId)
				.defaultHeader("X-Client-Secret", clientSecret)
				.build();
	}
}
