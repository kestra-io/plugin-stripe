package io.kestra.plugin.stripe.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Refund a payment in Stripe.",
    description = "Issue a full or partial refund for a payment using Stripe."
)
@Plugin(
    examples = {
        @Example(
            title = "Refund a payment by Charge ID",
            full = true,
            code = """
                id: refund_payment
                namespace: company.team

                tasks:
                  - id: refund_payment
                    type: io.kestra.plugin.stripe.payment.RefundPayment
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    chargeId: "ch_123456789"
                """
        ),
        @Example(
            title = "Refund a specific amount for a PaymentIntent",
            full = true,
            code = """
                id: refund_partial
                namespace: company.team

                tasks:
                  - id: refund_partial
                    type: io.kestra.plugin.stripe.payment.RefundPayment
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentIntentId: "pi_123456789"
                    amount: 500  # amount in cents
                """
        )
    }
)
public class RefundPayment extends AbstractStripe implements RunnableTask<RefundPayment.Output> {
    @Schema(
        title = "The ID of the Charge to refund. Either chargeId or paymentIntentId must be provided."
    )
    private Property<String> chargeId;

    @Schema(
        title = "The ID of the PaymentIntent to refund. Either chargeId or paymentIntentId must be provided."
    )
    private Property<String> paymentIntentId;

    @Schema(
        title = "The amount to refund in cents (optional). If not set, a full refund is issued."
    )
    private Property<Long> amount;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String apiKey = renderApiKey(runContext);

        String renderedChargeId = runContext.render(this.chargeId).as(String.class).orElse(null);
        String renderedPaymentIntentId = runContext.render(this.paymentIntentId).as(String.class).orElse(null);
        Long renderedAmount = runContext.render(this.amount).as(Long.class).orElse(null);

        if (renderedChargeId == null && renderedPaymentIntentId == null) {
            throw new IllegalArgumentException("Either chargeId or paymentIntentId must be provided.");
        }

        // Set API key
        com.stripe.Stripe.apiKey = apiKey;

        RefundCreateParams.Builder params = RefundCreateParams.builder();

        if (renderedChargeId != null) {
            params.setCharge(renderedChargeId);
        }
        if (renderedPaymentIntentId != null) {
            params.setPaymentIntent(renderedPaymentIntentId);
        }
        if (renderedAmount != null) {
            params.setAmount(renderedAmount);
        }

        try {
            Refund refund = Refund.create(params.build());

            return Output.builder()
                .refundId(refund.getId())
                .status(refund.getStatus())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .raw(refund.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create refund", e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the refund.")
        private final String refundId;

        @Schema(title = "The status of the refund.")
        private final String status;

        @Schema(title = "The refunded amount in cents.")
        private final Long amount;

        @Schema(title = "The currency of the refund.")
        private final String currency;

        @Schema(title = "The raw JSON response from Stripe.")
        private final String raw;
    }
}
