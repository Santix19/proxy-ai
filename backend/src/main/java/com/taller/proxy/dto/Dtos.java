package com.taller.proxy.dto;

import com.taller.proxy.model.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** DTOs del sistema agrupados para mantener el proyecto compacto. */
public final class Dtos {
    private Dtos() {}

    public record GenerationRequest(String userId, String prompt) {}

    public record GenerationResponse(String text, int promptTokens, int completionTokens, int totalTokens) {}

    /** Cuerpo de POST /api/ai/generate */
    public record GenerateBody(@NotBlank @Size(max = 4000) String prompt) {}

    /** Respuesta de POST /api/ai/generate */
    public record GenerateResult(String text, int promptTokens, int completionTokens, int totalTokens, QuotaStatus status) {}

    public record QuotaStatus(
            Plan plan,
            long tokensUsed,
            long tokensLimit,          // -1 = ilimitado
            long tokensRemaining,      // -1 = ilimitado
            int requestsThisMinute,
            int requestsLimit,         // -1 = ilimitado
            long secondsToRateReset,
            LocalDate resetDate,
            boolean unlimited) {}

    public record DailyUsage(LocalDate date, long tokens, long requests) {}

    public record UpgradeBody(Plan plan) {}

    public record ErrorBody(String error, String message, Long retryAfterSeconds) {}

    public record HistoryResponse(List<DailyUsage> days) {}
}
