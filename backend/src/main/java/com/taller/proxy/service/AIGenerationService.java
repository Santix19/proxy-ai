package com.taller.proxy.service;

import com.taller.proxy.dto.Dtos.GenerationRequest;
import com.taller.proxy.dto.Dtos.GenerationResponse;

/**
 * Subject del patrón Proxy: tanto el servicio real como los proxies
 * implementan esta misma interfaz, por lo que el cliente no distingue cuál usa.
 */
public interface AIGenerationService {
    GenerationResponse generate(GenerationRequest request);
}
