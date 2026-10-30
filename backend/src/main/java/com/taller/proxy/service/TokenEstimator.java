package com.taller.proxy.service;

/** Estimación simple: ~4 caracteres por token (mismo criterio que usa el frontend). */
public final class TokenEstimator {
    private TokenEstimator() {}

    public static int estimate(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }
}
