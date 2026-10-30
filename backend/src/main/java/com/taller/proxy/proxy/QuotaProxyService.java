package com.taller.proxy.proxy;

import com.taller.proxy.exception.QuotaExceededException;
import com.taller.proxy.service.AIGenerationService;
import com.taller.proxy.service.TokenEstimator;
import com.taller.proxy.store.UsageStore;
import com.taller.proxy.dto.Dtos.GenerationRequest;
import com.taller.proxy.dto.Dtos.GenerationResponse;

/**
 * Proxy #2: verifica que el usuario conserve cuota mensual de tokens.
 * Si la cuota está agotada (o el prompt no cabe) lanza {@link QuotaExceededException}
 * (HTTP 402). Tras la respuesta real, descuenta los tokens consumidos.
 */
public class QuotaProxyService implements AIGenerationService {

    private final AIGenerationService next;
    private final UsageStore store;

    public QuotaProxyService(AIGenerationService next, UsageStore store) {
        this.next = next;
        this.store = store;
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        long remaining = store.remainingTokens(request.userId());
        int estimated = TokenEstimator.estimate(request.prompt());
        if (remaining <= 0 || estimated > remaining) {
            throw new QuotaExceededException("Has agotado tu cuota mensual de tokens. Actualiza tu plan para continuar.");
        }
        GenerationResponse response = next.generate(request);
        store.deductTokens(request.userId(), response.totalTokens());
        return response;
    }
}
