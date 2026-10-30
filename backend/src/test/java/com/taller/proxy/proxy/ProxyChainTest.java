package com.taller.proxy.proxy;

import com.taller.proxy.dto.Dtos.GenerationRequest;
import com.taller.proxy.dto.Dtos.GenerationResponse;
import com.taller.proxy.exception.QuotaExceededException;
import com.taller.proxy.exception.RateLimitExceededException;
import com.taller.proxy.model.Plan;
import com.taller.proxy.service.AIGenerationService;
import com.taller.proxy.store.UsageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la cadena RateLimitProxy -> QuotaProxy -> servicio real.
 * Se usa un servicio real falso (sin Thread.sleep) que cuenta cuántas veces se invoca.
 */
class ProxyChainTest {

    private UsageStore store;
    private int realCalls;
    private AIGenerationService chain;

    @BeforeEach
    void setUp() {
        store = new UsageStore();
        realCalls = 0;
        AIGenerationService fakeReal = request -> {
            realCalls++;
            return new GenerationResponse("respuesta", 10, 90, 100);
        };
        chain = new RateLimitProxyService(new QuotaProxyService(fakeReal, store), store);
    }

    private GenerationResponse call(String user) {
        return chain.generate(new GenerationRequest(user, "hola mundo"));
    }

    @Test
    void requestValidaLlegaAlServicioRealYDescuentaTokens() {
        GenerationResponse r = call("u1");

        assertEquals(100, r.totalTokens());
        assertEquals(1, realCalls);
        assertEquals(100L, store.status("u1").tokensUsed());
        assertEquals(1, store.status("u1").requestsThisMinute());
    }

    @Test
    void planFreeBloqueaLaRequest11ConRetryAfter() {
        for (int i = 0; i < Plan.FREE.getRequestsPerMinute(); i++) {
            call("u2");
        }

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () -> call("u2"));

        assertTrue(ex.getRetryAfterSeconds() > 0 && ex.getRetryAfterSeconds() <= 60);
        assertEquals(10, realCalls, "El servicio real no debe ejecutarse cuando el proxy bloquea");
    }

    @Test
    void elResetDelMinutoPermiteVolverAEnviar() {
        for (int i = 0; i < Plan.FREE.getRequestsPerMinute(); i++) {
            call("u3");
        }
        assertThrows(RateLimitExceededException.class, () -> call("u3"));

        store.resetRateLimits();

        assertEquals(100, call("u3").totalTokens());
    }

    @Test
    void cuotaAgotadaRetornaQuotaExceededSinLlamarAlServicioReal() {
        store.exhaustQuota("u4");

        assertThrows(QuotaExceededException.class, () -> call("u4"));
        assertEquals(0, realCalls);
    }

    @Test
    void elRateLimitSeEvaluaAntesQueLaCuota() {
        store.exhaustQuota("u5");
        for (int i = 0; i < Plan.FREE.getRequestsPerMinute(); i++) {
            assertThrows(QuotaExceededException.class, () -> call("u5"));
        }

        assertThrows(RateLimitExceededException.class, () -> call("u5"));
    }

    @Test
    void upgradeAProAumentaLimitesYResetMensualLiberaCuota() {
        store.exhaustQuota("u6");
        store.upgrade("u6", Plan.PRO);

        assertEquals(Plan.PRO, store.status("u6").plan());
        assertEquals(100, call("u6").totalTokens());

        store.resetMonthlyQuota();
        assertEquals(0L, store.status("u6").tokensUsed());
    }

    @Test
    void noSePuedeHacerDowngrade() {
        store.upgrade("u7", Plan.PRO);

        assertThrows(IllegalArgumentException.class, () -> store.upgrade("u7", Plan.FREE));
    }

    @Test
    void enterpriseEsIlimitado() {
        store.upgrade("u8", Plan.ENTERPRISE);
        for (int i = 0; i < 200; i++) {
            call("u8");
        }

        assertEquals(200, realCalls);
        assertTrue(store.status("u8").unlimited());
    }

    @Test
    void historialDevuelveSieteDiasConElUsoDeHoy() {
        call("u9");

        var days = store.history("u9", 7);

        assertEquals(7, days.size());
        assertEquals(100L, days.get(6).tokens());
        assertEquals(1L, days.get(6).requests());
    }
}
