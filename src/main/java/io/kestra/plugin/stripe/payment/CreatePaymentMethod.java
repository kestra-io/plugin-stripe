package io.kestra.plugin.stripe.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stripe.AbstractStripe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a new PaymentMethod in Stripe",
    description = "This task creates a new PaymentMethod such as card, SEPA debit, etc."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a card payment method",
            full = true,
            code = """
                id: create_card_pm
                namespace: company.team

                tasks:
                  - id: create_card_pm
                    type: io.kestra.plugin.stripe.payment.CreatePaymentMethod
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    type: "card"
                    card:
                      number: "4242424242424242"
                      exp_month: 12
                      exp_year: 2026
                      cvc: "123"
                """
        )
    }
)
public class CreatePaymentMethod extends AbstractStripe implements RunnableTask<CreatePaymentMethod.Output> {

    @Schema(title = "The type of payment method (card, sepa_debit, etc.)")
    @NotNull
    private Property<String> type;

    @Schema(title = "Additional details for the payment method (card, sepa info, etc.)")
    private Property<Map<String, Object>> card;

    @Schema(title = "Metadata attached to the payment method")
    private Property<Map<String, String>> metadata;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String apiKey = renderApiKey(runContext);
        Stripe.apiKey = apiKey;

        String renderedType = runContext.render(type).as(String.class).orElseThrow();
        Map<String, Object> renderedCard = runContext.render(card).as(Map.class).orElse(null);
        Map<String, String> renderedMetadata = runContext.render(metadata).as(Map.class).orElse(null);

        PaymentMethodCreateParams.Builder paramsBuilder = PaymentMethodCreateParams.builder()
            .setType(renderedType);

        if ("card".equals(renderedType) && renderedCard != null) {
            PaymentMethodCreateParams.CardDetails.Builder cardBuilder = PaymentMethodCreateParams.CardDetails.builder()
                .setNumber((String) renderedCard.get("number"))
                .setExpMonth(Long.valueOf(renderedCard.get("exp_month").toString()))
                .setExpYear(Long.valueOf(renderedCard.get("exp_year").toString()))
                .setCvc((String) renderedCard.get("cvc"));
            paramsBuilder.setCard(cardBuilder.build());
        }

        if (renderedMetadata != null) {
            paramsBuilder.putAllMetadata(renderedMetadata);
        }

        try {
            PaymentMethod paymentMethod = PaymentMethod.create(paramsBuilder.build());

            return Output.builder()
                .id(paymentMethod.getId())
                .type(paymentMethod.getType())
                .billingDetails(paymentMethod.getBillingDetails().getAddress() != null ? paymentMethod.getBillingDetails().getAddress().toMap() : null)
                .raw(paymentMethod.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create PaymentMethod", e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the created PaymentMethod")
        private final String id;

        @Schema(title = "The type of the PaymentMethod")
        private final String type;

        @Schema(title = "Billing details (if any)")
        private final Map<String, Object> billingDetails;

        @Schema(title = "Raw JSON returned from Stripe")
        private final String raw;
    }
}
