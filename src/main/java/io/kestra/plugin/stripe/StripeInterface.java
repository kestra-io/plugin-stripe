package io.kestra.plugin.stripe;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Common interface for Stripe plugin tasks using Stripe Java SDK.
 */
public interface StripeInterface {
    /**
     * Stripe API key (secret).
     */
    @Schema(
        title = "Stripe API Key",
        description = "Secret key for authenticating with Stripe. Starts with 'sk_' for live/test keys."
    )
    @NotNull
    Property<String> getApiKey();
}
