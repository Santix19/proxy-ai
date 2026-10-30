package com.taller.proxy.controller;

import com.taller.proxy.model.Plan;
import com.taller.proxy.store.UsageStore;
import com.taller.proxy.dto.Dtos.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final UsageStore store;

    public QuotaController(UsageStore store) {
        this.store = store;
    }

    @GetMapping("/status")
    public QuotaStatus status(@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        return store.status(userId);
    }

    @GetMapping("/history")
    public HistoryResponse history(@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        return new HistoryResponse(store.history(userId, 7));
    }

    @PostMapping("/upgrade")
    public QuotaStatus upgrade(@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
                               @RequestBody(required = false) UpgradeBody body) {
        Plan target = (body == null || body.plan() == null) ? Plan.PRO : body.plan();
        store.upgrade(userId, target);
        return store.status(userId);
    }

    /** Endpoint auxiliar SOLO para la demostración en clase: agota la cuota mensual del usuario. */
    @PostMapping("/demo/exhaust")
    public QuotaStatus exhaust(@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        store.exhaustQuota(userId);
        return store.status(userId);
    }
}
