package io.kestra.plugin.stripe;

import com.stripe.StripeClient;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base class for Stripe tasks using the Stripe Java SDK.
 */
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractStripe extends Task implements StripeInterface {
    /**
     * Stripe API key (secret).
     */
    @NotNull
    protected Property<String> apiKey;

    /**
     * Returns a configured Stripe client.
     */
    protected StripeClient client(RunContext runContext) {
        String renderedApiKey = runContext.render(this.apiKey)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));

        return new StripeClient(renderedApiKey);
    }
}
