package com.taller.proxy.model;

/**
 * Planes disponibles. Un valor -1 significa "ilimitado".
 */
public enum Plan {
    FREE(10, 50_000L),
    PRO(60, 500_000L),
    ENTERPRISE(-1, -1L);

    private final int requestsPerMinute;
    private final long tokensPerMonth;

    Plan(int requestsPerMinute, long tokensPerMonth) {
        this.requestsPerMinute = requestsPerMinute;
        this.tokensPerMonth = tokensPerMonth;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public long getTokensPerMonth() {
        return tokensPerMonth;
    }

    public boolean isUnlimitedRequests() {
        return requestsPerMinute < 0;
    }

    public boolean isUnlimitedTokens() {
        return tokensPerMonth < 0;
    }
}
