package com.taller.proxy.store;

import com.taller.proxy.model.Plan;
import com.taller.proxy.dto.Dtos.DailyUsage;
import com.taller.proxy.dto.Dtos.QuotaStatus;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado en memoria del consumo por usuario (plan, requests del minuto,
 * tokens del mes e historial diario). Es thread-safe por usuario.
 */
@Component
public class UsageStore {

    static final ZoneId ZONE = ZoneOffset.UTC;

    private static class UserUsage {
        Plan plan = Plan.FREE;
        int requestsThisMinute = 0;
        long tokensUsed = 0;
        final TreeMap<LocalDate, long[]> daily = new TreeMap<>(); // [tokens, requests]
    }

    private final Map<String, UserUsage> users = new ConcurrentHashMap<>();

    private UserUsage of(String userId) {
        return users.computeIfAbsent(userId, id -> new UserUsage());
    }

    private static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static long secondsToNextMinute() {
        long now = Instant.now().getEpochSecond();
        return 60 - (now % 60);
    }

    public static LocalDate nextResetDate() {
        return today().withDayOfMonth(1).plusMonths(1);
    }

    // ---------- RateLimit ----------

    /** @return 0 si la request fue permitida; si no, segundos para reintentar. */
    public long tryConsumeRequest(String userId) {
        UserUsage u = of(userId);
        synchronized (u) {
            Plan p = u.plan;
            if (!p.isUnlimitedRequests() && u.requestsThisMinute >= p.getRequestsPerMinute()) {
                return secondsToNextMinute();
            }
            u.requestsThisMinute++;
            u.daily.computeIfAbsent(today(), d -> new long[2])[1]++;
            return 0;
        }
    }

    public void resetRateLimits() {
        users.values().forEach(u -> {
            synchronized (u) {
                u.requestsThisMinute = 0;
            }
        });
    }

    // ---------- Quota ----------

    public long remainingTokens(String userId) {
        UserUsage u = of(userId);
        synchronized (u) {
            if (u.plan.isUnlimitedTokens()) return Long.MAX_VALUE;
            return Math.max(0, u.plan.getTokensPerMonth() - u.tokensUsed);
        }
    }

    public void deductTokens(String userId, long tokens) {
        UserUsage u = of(userId);
        synchronized (u) {
            u.tokensUsed += tokens;
            u.daily.computeIfAbsent(today(), d -> new long[2])[0] += tokens;
        }
    }

    public void resetMonthlyQuota() {
        users.values().forEach(u -> {
            synchronized (u) {
                u.tokensUsed = 0;
            }
        });
    }

    /**
     * Solo para la demo: deja la cuota mensual a punto de agotarse (quedan 0 tokens)
     * para poder mostrar el 402 y el modal de upgrade sin enviar cientos de prompts.
     */
    public void exhaustQuota(String userId) {
        UserUsage u = of(userId);
        synchronized (u) {
            if (!u.plan.isUnlimitedTokens()) {
                u.tokensUsed = u.plan.getTokensPerMonth();
            }
        }
    }

    // ---------- Plan ----------

    public Plan upgrade(String userId, Plan target) {
        UserUsage u = of(userId);
        synchronized (u) {
            if (target.ordinal() <= u.plan.ordinal()) {
                throw new IllegalArgumentException(
                        "Solo puedes pasar a un plan superior a " + u.plan + ".");
            }
            u.plan = target;
            return u.plan;
        }
    }

    // ---------- Consultas ----------

    public QuotaStatus status(String userId) {
        UserUsage u = of(userId);
        synchronized (u) {
            Plan p = u.plan;
            boolean unlimited = p.isUnlimitedTokens();
            long limit = p.getTokensPerMonth();
            long remaining = unlimited ? -1 : Math.max(0, limit - u.tokensUsed);
            return new QuotaStatus(p, u.tokensUsed, limit, remaining,
                    u.requestsThisMinute, p.getRequestsPerMinute(),
                    secondsToNextMinute(), nextResetDate(), unlimited);
        }
    }

    public List<DailyUsage> history(String userId, int days) {
        UserUsage u = of(userId);
        synchronized (u) {
            List<DailyUsage> out = new ArrayList<>();
            LocalDate end = today();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate d = end.minusDays(i);
                long[] v = u.daily.getOrDefault(d, new long[2]);
                out.add(new DailyUsage(d, v[0], v[1]));
            }
            return out;
        }
    }
}
