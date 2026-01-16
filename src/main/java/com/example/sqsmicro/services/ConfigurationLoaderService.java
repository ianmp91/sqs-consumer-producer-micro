package com.example.sqsmicro.services;

import com.example.sqsmicro.records.ConfigServerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ian.paris
 * @since 2026-01-13
 */
@Service("configurationLoaderService")
public class ConfigurationLoaderService {

	private final RestClient restClient;

	// Identidad propia
	private static final String MY_SERVICE_ID = "airport-c";

	// Cache segregada: Key = Nombre del Servicio (ej: 'airport-c'), Value = Sus propiedades
	private final Map<String, Map<String, Object>> servicesConfigurationCache = new ConcurrentHashMap<>();

	// Set de servicios que queremos mantener actualizados automáticamente con el @Scheduled
	private final Set<String> trackedServices = ConcurrentHashMap.newKeySet();

	public ConfigurationLoaderService(@Qualifier("configRestClient") RestClient restClient) {
		this.restClient = restClient;
		// Al iniciar, siempre queremos rastrear nuestra propia config
		this.trackedServices.add(MY_SERVICE_ID);
	}

	/**
	 * Carga/Refresca la configuración de un servicio específico.
	 * Retorna el mapa de propiedades para uso inmediato si se desea.
	 */
	public Map<String, Object> loadConfigurationFor(String serviceName) {
		try {
			var response = restClient.get()
					.uri("/{name}/default", serviceName)
					.retrieve()
					.body(ConfigServerResponse.class);

			if (response != null && response.propertySources() != null) {
				var newProperties = new HashMap<String, Object>();

				// Lógica de prioridad de Spring (Sources vienen en orden de prioridad descendente)
				// Iteramos en reverso para que las más altas sobrescriban a las bajas
				var reversedSources = new ArrayList<>(response.propertySources());
				Collections.reverse(reversedSources);

				for (var source : reversedSources) {
					newProperties.putAll(source.source());
				}

				servicesConfigurationCache.put(serviceName, newProperties);
				trackedServices.add(serviceName); // Lo agregamos a la lista de refresh automático

				System.out.println("Config updated for: " + serviceName);
				return newProperties;
			}
		} catch (Exception e) {
			System.err.println("Error fetching config for " + serviceName + ": " + e.getMessage());
			// En caso de error, podríamos retornar la caché vieja si existe, o lanzar exepción
		}
		return Map.of();
	}

	@Scheduled(fixedRate = 600000) // 10 minutos
	public void refreshAllTrackedConfigurations() {
		// Refresca la config propia y la de cualquier peer que hayamos consultado antes
		trackedServices.forEach(this::loadConfigurationFor);
	}

	private Object getProperty(String serviceName, String key) {
		// Si no tenemos la config de ese servicio, intentamos cargarla por primera vez (Lazy Loading)
		if (!servicesConfigurationCache.containsKey(serviceName)) {
			loadConfigurationFor(serviceName);
		}

		var props = servicesConfigurationCache.get(serviceName);
		return (props != null) ? props.get(key) : null;
	}


	public String getMyListeningQueue() {
		return (String) getProperty(MY_SERVICE_ID, "config.queues.inbound");
	}

	public String getPeerPublicKey(String targetServiceName) {
		return (String) getProperty(targetServiceName, "config.security.public-key");
	}

	public String getPeerTargetQueue(String targetServiceName) {
		return (String) getProperty(targetServiceName, "config.queues.inbound");
	}
}