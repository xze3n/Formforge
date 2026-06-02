package com.formforge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Thin client for the local Ollama REST API.
 *
 * <p>Called from {@link ThreatDetectionService} after an observation is
 * persisted to generate a human-readable security analysis. The call is
 * always made from an async thread ({@code auditTaskExecutor}) so it never
 * blocks incoming HTTP requests.
 *
 * <p>If Ollama is unavailable or the model times out the method returns
 * {@code null} gracefully — detection continues to work without AI.
 */
@Slf4j
@Service
public class OllamaService {

    private final String ollamaUrl;
    private final String ollamaModel;
    private final RestTemplate restTemplate;

    public OllamaService(
            @Value("${ollama.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ollama.model:codellama:7b}")         String ollamaModel) {
        this.ollamaUrl   = ollamaUrl;
        this.ollamaModel = ollamaModel;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Asks the local LLM to analyse a flagged threat and return a 2-3 sentence
     * explanation suitable for display in the admin dashboard.
     *
     * @param username the affected user's display name
     * @param reason   the rule that fired (e.g. {@code BRUTE_FORCE_LOGIN})
     * @param details  the trigger summary (e.g. "6 failed logins in 10 minutes")
     * @return the model's response string, or {@code null} on any error
     */
    @SuppressWarnings("unchecked")
    public String explain(String username, String reason, String details) {
        String prompt = String.format(
                "You are a security analyst reviewing suspicious activity on a web application. " +
                "User '%s' has triggered the threat detection rule '%s'. " +
                "Trigger details: %s. " +
                "In 2-3 concise sentences: explain why this behaviour is suspicious, " +
                "what type of attack or abuse it could represent, " +
                "and what immediate action the administrator should consider taking.",
                username, reason, details);

        Map<String, Object> body = Map.of(
                "model",  ollamaModel,
                "prompt", prompt,
                "stream", false);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    ollamaUrl + "/api/generate", body, Map.class);
            if (response != null && response.get("response") instanceof String text) {
                return text.strip();
            }
            return null;
        } catch (Exception ex) {
            log.warn("[OllamaService] explanation failed ({}): {}", ollamaModel, ex.getMessage());
            return null;
        }
    }
}
