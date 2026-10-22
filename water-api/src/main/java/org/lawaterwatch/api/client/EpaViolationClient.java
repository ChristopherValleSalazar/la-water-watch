package org.lawaterwatch.api.client;

import org.lawaterwatch.api.dto.EpaViolation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

//EPA SDWIS violation history, one call per water system
@Component
public class EpaViolationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EpaViolationClient(@Value("${app.epa.url}") String url, ObjectMapper objectMapper) {
        this.restClient = RestClients.create(url);
        this.objectMapper = objectMapper;
    }

    //An empty array is a normal answer, a system with no violations
    public List<EpaViolation> fetchViolations(String pwsid) {
        String body = restClient.get()
                .uri("/VIOLATION/PWSID/{pwsid}/JSON", pwsid)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return List.of();
        }
        return List.of(objectMapper.readValue(body, EpaViolation[].class));
    }
}
