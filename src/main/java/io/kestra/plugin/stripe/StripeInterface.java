package io.kestra.plugin.stripe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Common interface for Stripe plugin tasks.
 */
public interface StripeInterface {
    /**
     * Stripe API base URL (defaults to https://api.stripe.com).
     */
    @Schema(
        title = "Base URL for Stripe API",
        description = "Defaults to https://api.stripe.com. Usually no need to change unless testing against a mock server."
    )
    @NotNull
    Property<String> getUrl();

    /**
     * Stripe API key (secret).
     */
    @Schema(
        title = "Stripe API Key",
        description = "Secret key for authenticating with Stripe. Starts with 'sk_' for live/test keys."
    )
    @NotNull
    Property<String> getApiKey();

    /**
     * Optional HTTP configuration (timeouts, retries, etc.).
     */
    @Schema(
        title = "HTTP Options",
        description = "Optional advanced configuration for HTTP client (timeouts, retries, proxy, etc.)."
    )
    @JsonIgnore
    HttpConfiguration getOptions();
}
