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
                    type: io.kestra.plugin.stripe.payment.ConfirmIntent
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentIntentId: "pi_123456789"
                """
        )
    }
)
public class ConfirmIntent extends AbstractStripe implements RunnableTask<ConfirmIntent.Output> {

    @Schema(title = "The ID of the PaymentIntent to confirm.")
    @NotNull
    private Property<String> paymentIntentId;

    @Schema(title = "Optional payment method ID to use for confirmation.")
    private Property<String> paymentMethod;

    @Schema(title = "Optional return URL for redirect-based payment methods.")
    private Property<String> returnUrl;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve PaymentIntent ID
        String renderedId = runContext.render(this.paymentIntentId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("PaymentIntent ID is required"));

        // Resolve optional parameters
        String renderedPaymentMethod = this.paymentMethod != null
            ? runContext.render(this.paymentMethod).as(String.class).orElse(null)
            : null;

        String renderedReturnUrl = this.returnUrl != null
            ? runContext.render(this.returnUrl).as(String.class).orElse(null)
            : null;

        try {
            // Build confirm params
            PaymentIntentConfirmParams.Builder paramsBuilder = PaymentIntentConfirmParams.builder();

            if (renderedPaymentMethod != null) {
                paramsBuilder.setPaymentMethod(renderedPaymentMethod);
            }

            if (renderedReturnUrl != null) {
                paramsBuilder.setReturnUrl(renderedReturnUrl);
            }

            // Use the client from AbstractStripe
            PaymentIntent confirmed = client(runContext).paymentIntents().confirm(
                renderedId,
                paramsBuilder.build()
            );

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