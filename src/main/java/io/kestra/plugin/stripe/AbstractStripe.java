package io.kestra.plugin.stripe;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for Stripe tasks providing common functionality.
 */
@Slf4j
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractStripe extends Task implements StripeInterface {
    /**
     * Base URL for Stripe API. Usually "https://api.stripe.com"
     */
    @Builder.Default
    protected Property<String> url = Property.ofValue("https://api.stripe.com");

    /**
     * Stripe API key (secret).
     */
    @NotNull
    protected Property<String> apiKey;

    /**
     * Optional HTTP configuration for client.
     */
    @Builder.Default
    protected HttpConfiguration options = HttpConfiguration.builder().build();

    /**
     * Creates an HTTP client configured for Stripe API calls.
     */
    protected HttpClient client(RunContext runContext)
            throws IllegalVariableEvaluationException, MalformedURLException, URISyntaxException {
        return HttpClient.builder()
            .configuration(this.options)
            .runContext(runContext)
            .build();
    }

    /**
     * Creates a base HTTP request with common Stripe headers.
     */
    protected HttpRequest.HttpRequestBuilder baseRequest(RunContext runContext, String endpoint)
            throws IllegalVariableEvaluationException, URISyntaxException {
        String renderedUrl = runContext.render(this.url)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Stripe API URL is required"));

        String renderedApiKey = runContext.render(this.apiKey)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));

        // Ensure base URL ends with /v1 (Stripe REST API)
        if (!renderedUrl.endsWith("/v1")) {
            if (renderedUrl.endsWith("/")) {
                renderedUrl = renderedUrl + "v1";
            } else {
                renderedUrl = renderedUrl + "/v1";
            }
        }

        URI fullUri = new URI(renderedUrl + endpoint);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Authorization", List.of("Bearer " + renderedApiKey));
        headers.put("Content-Type", List.of("application/x-www-form-urlencoded"));
        headers.put("Accept", List.of("application/json"));

        log.debug("Stripe request URI: {}", fullUri);

        return HttpRequest.builder()
            .uri(fullUri)
            .headers(HttpHeaders.of(headers, (a, b) -> true));
    }

    /**
     * Builds the endpoint for a resource (e.g. /customers, /charges).
     */
    protected String buildResourceEndpoint(String resource) {
        return "/" + resource;
    }

    /**
     * Builds the endpoint for a specific resource by ID.
     */
    protected String buildResourceEndpoint(String resource, String id) {
        return "/" + resource + "/" + id;
    }
}
