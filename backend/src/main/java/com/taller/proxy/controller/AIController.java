package com.taller.proxy.controller;

import com.taller.proxy.service.AIGenerationService;
import com.taller.proxy.store.UsageStore;
import com.taller.proxy.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIGenerationService service; // = cadena completa de proxies
    private final UsageStore store;

    public AIController(AIGenerationService service, UsageStore store) {
        this.service = service;
        this.store = store;
    }

    @PostMapping("/generate")
    public GenerateResult generate(@RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
                                   @Valid @RequestBody GenerateBody body) {
        GenerationResponse r = service.generate(new GenerationRequest(userId, body.prompt()));
        return new GenerateResult(r.text(), r.promptTokens(), r.completionTokens(), r.totalTokens(), store.status(userId));
    }
}
