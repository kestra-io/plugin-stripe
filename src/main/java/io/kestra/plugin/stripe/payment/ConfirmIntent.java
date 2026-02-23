package io.kestra.plugin.stripe.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Confirm a Stripe PaymentIntent",
    description = "Confirms a PaymentIntent by ID, optionally specifying a PaymentMethod and return URL for redirect flows. Uses the secret key context."
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
                    type: io.kestra.plugin.stripe.payment.ConfirmIntent
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentIntentId: "pi_123456789"
                """
        )
    }
)
public class ConfirmIntent extends AbstractStripe implements RunnableTask<ConfirmIntent.Output> {

    @Schema(title = "PaymentIntent ID to confirm")
    @NotNull
    private Property<String> paymentIntentId;

    @Schema(title = "PaymentMethod ID", description = "Optional PaymentMethod to use when confirming")
    private Property<String> paymentMethod;

    @Schema(title = "Return URL", description = "Optional redirect URL for SCA/redirect flows")
    private Property<String> returnUrl;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve PaymentIntent ID
        String rId = runContext.render(this.paymentIntentId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("PaymentIntent ID is required"));

        // Resolve optional parameters
        String rPaymentMethod = runContext.render(this.paymentMethod).as(String.class).orElse(null);

        String rReturnUrl = runContext.render(this.returnUrl).as(String.class).orElse(null);

        try {
            // Build confirm params
            PaymentIntentConfirmParams.Builder paramsBuilder = PaymentIntentConfirmParams.builder();

            if (rPaymentMethod != null) {
                paramsBuilder.setPaymentMethod(rPaymentMethod);
            }

            if (rReturnUrl != null) {
                paramsBuilder.setReturnUrl(rReturnUrl);
            }

            // Use the client from AbstractStripe
            PaymentIntent confirmed = client(runContext).paymentIntents().confirm(
                rId,
                paramsBuilder.build()
            );

            return Output.builder()
                .paymentIntentId(confirmed.getId())
                .status(confirmed.getStatus())
                .raw(confirmed.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to confirm PaymentIntent: " + rId, e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Confirmed PaymentIntent ID")
        private final String paymentIntentId;

        @Schema(title = "PaymentIntent status")
        private final String status;

        @Schema(title = "Raw Stripe response")
        private final String raw;
    }
}
