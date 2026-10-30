package com.taller.proxy.scheduler;

import com.taller.proxy.store.UsageStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tareas programadas: reset del rate limit cada minuto y de la cuota el día 1 de cada mes. */
@Component
public class ResetScheduler {

    private final UsageStore store;

    public ResetScheduler(UsageStore store) {
        this.store = store;
    }

    @Scheduled(cron = "0 * * * * *", zone = "UTC")
    public void resetRateLimit() {
        store.resetRateLimits();
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    public void resetMonthlyQuota() {
        store.resetMonthlyQuota();
    }
}
