package com.example.sqsmicro.records;

import java.util.Map;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
public record MessageDto(
        Map<String, String> metadata,
        String encryptedPayload,
        String keyId
) {}
