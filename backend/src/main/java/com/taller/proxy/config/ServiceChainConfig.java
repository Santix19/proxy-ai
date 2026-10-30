package com.taller.proxy.config;

import com.taller.proxy.proxy.QuotaProxyService;
import com.taller.proxy.proxy.RateLimitProxyService;
import com.taller.proxy.service.AIGenerationService;
import com.taller.proxy.service.MockAIGenerationService;
import com.taller.proxy.store.UsageStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma la cadena de proxies:
 * request -> RateLimitProxyService -> QuotaProxyService -> MockAIGenerationService
 */
@Configuration
public class ServiceChainConfig {

    @Bean
    public AIGenerationService aiGenerationService(UsageStore store) {
        AIGenerationService real = new MockAIGenerationService();
        AIGenerationService quota = new QuotaProxyService(real, store);
        return new RateLimitProxyService(quota, store);
    }
}
