package com.example.sqsmicro.service;

import com.example.sqsmicro.records.ConfigServerResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ian.paris
 * @since 2026-01-13
 */
@Service("externalConfigService")
public class ExternalConfigService {

	private final RestClient restClient;
	// @Value("${config.server.url:http://localhost:8888}")
	private final String configServerUrl = "http://localhost:8888";

	// Caché en memoria para no saturar al Config Server
	private Map<String, Object> configurationCache = new ConcurrentHashMap<>();

	public ExternalConfigService() {
		this.restClient = RestClient.builder()
				.baseUrl(configServerUrl)
				.build();
	}

	@Scheduled(fixedRate = 600000)
	public void fetchConfiguration() {
		// 'airlines-b' es el nombre del micro, 'default' es el perfil
		var response = restClient.get()
				.uri("/airport-c/default")
				.retrieve()
				.body(ConfigServerResponse.class);

		if (response != null && response.propertySources() != null) {
			// Spring Config devuelve las fuentes en orden de prioridad (la 0 es la más alta)
			// Iteramos en reverso para llenar el mapa y que las prioridades altas sobrescriban a las bajas
			var reversedSources = new ArrayList<>(response.propertySources());
			Collections.reverse(reversedSources);

			for (var source : reversedSources) {
				configurationCache.putAll(source.source());
			}
			System.out.println("Config updated from Server D");
		}
	}

	public String getProperty(String key) {
		if (configurationCache.isEmpty()) {
			fetchConfiguration();
		}
		return (String) configurationCache.get(key);
	}

	public String getAirportPublicKey() {
		return getProperty("config.security.target-public-key");
	}

	public String getQueueUrl() {
		return getProperty("config.queues.outbound");
	}

	public String getListeningQueue() {
		return getProperty("config.queues.inbound");
	}
}
