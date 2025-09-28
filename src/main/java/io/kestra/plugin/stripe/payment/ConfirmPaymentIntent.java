package io.kestra.plugin.stripe.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Confirm a PaymentIntent in Stripe.",
    description = "This task confirms a PaymentIntent to process a payment."
)
@Plugin(
    examples = {
        @Example(
            title = "Confirm a PaymentIntent",
            full = true,
            code = """
                id: confirm_payment_intent
                namespace: company.team

                tasks:
                  - id: confirm_payment
                    type: io.kestra.plugin.stripe.payment.ConfirmPaymentIntent
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentIntentId: "pi_123456789"
                """
        )
    }
)
public class ConfirmPaymentIntent extends AbstractStripe implements RunnableTask<ConfirmPaymentIntent.Output> {

    @Schema(title = "The ID of the PaymentIntent to confirm.")
    @NotNull
    private Property<String> paymentIntentId;

    @Schema(title = "Optional parameters to pass when confirming the PaymentIntent.")
    private Property<Map<String, Object>> params;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve API key
        String apiKey = runContext.render(this.apiKey)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));
        Stripe.apiKey = apiKey;

        // Resolve PaymentIntent ID
        String renderedId = runContext.render(this.paymentIntentId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("PaymentIntent ID is required"));

        // Resolve params (optional, defaults to empty map)
        Map<String, Object> renderedParams = this.params != null
            ? runContext.render(this.params).asMap(String.class, Object.class)
            : new HashMap<>();

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(renderedId);
            PaymentIntent confirmed = paymentIntent.confirm(renderedParams);

            return Output.builder()
                .paymentIntentId(confirmed.getId())
                .status(confirmed.getStatus())
                .clientSecret(confirmed.getClientSecret())
                .raw(confirmed.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to confirm PaymentIntent: " + renderedId, e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the confirmed PaymentIntent.")
        private final String paymentIntentId;

        @Schema(title = "The status of the PaymentIntent.")
        private final String status;

        @Schema(title = "The client secret associated with the PaymentIntent.")
        private final String clientSecret;

        @Schema(title = "The raw JSON response from Stripe.")
        private final String raw;
    }
}
