package io.kestra.plugin.stripe.payment;

import com.stripe.exception.StripeException;
import com.stripe.param.RefundCreateParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
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
                    type: io.kestra.plugin.stripe.payment.Refund
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
public class Refund extends AbstractStripe implements RunnableTask<Refund.Output> {
    @Schema(
        title = "The charge ID to refund. Either `chargeId` or `paymentIntentId` must be provided."
    )
    private Property<String> chargeId;

    @Schema(
        title = "The PaymentIntent ID to refund. Either `chargeId` or `paymentIntentId` must be provided."
    )
    private Property<String> paymentIntentId;

    @Schema(
        title = "The amount to refund in cents (optional). If not set, a full refund is issued."
    )
    private Property<Long> amount;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String rChargeId = runContext.render(this.chargeId).as(String.class).orElse(null);
        String rPaymentIntentId = runContext.render(this.paymentIntentId).as(String.class).orElse(null);
        Long rAmount = runContext.render(this.amount).as(Long.class).orElse(null);

        if (rChargeId == null && rPaymentIntentId == null) {
            throw new IllegalArgumentException("Either chargeId or paymentIntentId must be provided.");
        }

        RefundCreateParams.Builder params = RefundCreateParams.builder();

        if (rChargeId != null) {
            params.setCharge(rChargeId);
        }
        if (rPaymentIntentId != null) {
            params.setPaymentIntent(rPaymentIntentId);
        }
        if (rAmount != null) {
            params.setAmount(rAmount);
        }

        try {
            // Use the client from AbstractStripe
            com.stripe.model.Refund refund = client(runContext).refunds().create(params.build());

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
        @Schema(title = "The refund ID")
        private final String refundId;

        @Schema(title = "The refund status")
        private final String status;

        @Schema(title = "The refunded amount in cents")
        private final Long amount;

        @Schema(title = "The refund currency")
        private final String currency;

        @Schema(title = "The raw JSON response from Stripe")
        private final String raw;
    }
}