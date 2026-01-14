package com.example.sqsmicro.records;

import java.util.List;
import java.util.Map;

/**
 * @author ian.paris
 * @since 2026-01-13
 */
public record ConfigServerResponse(String name, List<PropertySource> propertySources) {
	public record PropertySource(String name, Map<String, Object> source) {}
}
