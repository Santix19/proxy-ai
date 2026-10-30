package com.taller.proxy.proxy;

import com.taller.proxy.exception.RateLimitExceededException;
import com.taller.proxy.service.AIGenerationService;
import com.taller.proxy.store.UsageStore;
import com.taller.proxy.dto.Dtos.GenerationRequest;
import com.taller.proxy.dto.Dtos.GenerationResponse;

/**
 * Proxy #1: verifica que el usuario no supere N requests por minuto según su plan.
 * Si supera el límite lanza {@link RateLimitExceededException} (HTTP 429 + Retry-After).
 */
public class RateLimitProxyService implements AIGenerationService {

    private final AIGenerationService next;
    private final UsageStore store;

    public RateLimitProxyService(AIGenerationService next, UsageStore store) {
        this.next = next;
        this.store = store;
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        long retryAfter = store.tryConsumeRequest(request.userId());
        if (retryAfter > 0) {
            throw new RateLimitExceededException(retryAfter);
        }
        return next.generate(request);
    }
}
