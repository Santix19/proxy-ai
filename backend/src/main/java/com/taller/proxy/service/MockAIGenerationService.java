package com.taller.proxy.service;

import com.taller.proxy.dto.Dtos.GenerationRequest;
import com.taller.proxy.dto.Dtos.GenerationResponse;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RealSubject del patrón Proxy: simula una generación de texto de IA.
 * NO es un @Service para que Spring no la inyecte directamente;
 * se construye en ServiceChainConfig al final de la cadena.
 */
public class MockAIGenerationService implements AIGenerationService {

    private static final List<String> SENTENCES = List.of(
            "El patrón Proxy permite controlar el acceso a un objeto sin modificar su interfaz.",
            "Un proxy de rate limiting protege al servicio real de ráfagas de peticiones.",
            "Las cuotas mensuales por plan permiten monetizar el consumo de la plataforma.",
            "Encadenar proxies facilita aplicar responsabilidades independientes y reutilizables.",
            "El servicio real solo se ejecuta cuando todos los proxies previos autorizan la solicitud.",
            "Separar la lógica de control de la lógica de negocio mejora la mantenibilidad del sistema.",
            "Cada plan define un límite de requests por minuto y de tokens por mes.",
            "Este texto fue generado por un servicio de IA simulado con fines académicos.");

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int sentences = rnd.nextInt(2, 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentences; i++) {
            if (i > 0) sb.append(' ');
            sb.append(SENTENCES.get(rnd.nextInt(SENTENCES.size())));
        }
        String text = sb.toString();
        int promptTokens = TokenEstimator.estimate(request.prompt());
        int completionTokens = TokenEstimator.estimate(text);
        return new GenerationResponse(text, promptTokens, completionTokens, promptTokens + completionTokens);
    }
}
