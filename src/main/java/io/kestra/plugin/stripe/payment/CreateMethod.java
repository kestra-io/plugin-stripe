package io.kestra.plugin.stripe.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.stripe.AbstractStripe;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a PaymentMethod in Stripe.",
    description = "This task creates a PaymentMethod, typically with card details."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a card PaymentMethod",
            full = true,
            code = """
                id: create_payment_method
                namespace: company.team

                tasks:
                  - id: create_payment_method
                    type: io.kestra.plugin.stripe.payment.CreateMethod
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    type: "card"
                    cardNumber: "4242424242424242"
                    expMonth: 12
                    expYear: 2025
                    cvc: "123"
                """
        )
    }
)
public class CreateMethod extends AbstractStripe implements RunnableTask<CreateMethod.Output> {
    @NotNull
    @Schema(title = "The type of the PaymentMethod. Example: `card`")
    private Property<String> paymentMethodType;

    @Schema(title = "Card number (required if type = card).")
    private Property<String> cardNumber;

    @Schema(title = "Card expiration month.")
    private Property<Long> expMonth;

    @Schema(title = "Card expiration year.")
    private Property<Long> expYear;

    @Schema(title = "Card CVC.")
    private Property<String> cvc;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve inputs
        String rType = runContext.render(this.paymentMethodType).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("PaymentMethod type is required"));

        PaymentMethodCreateParams.Builder builder = PaymentMethodCreateParams.builder()
            .setType(PaymentMethodCreateParams.Type.valueOf(rType.toUpperCase()));

        if ("card".equalsIgnoreCase(rType)) {
            String number = runContext.render(this.cardNumber).as(String.class)
                .orElseThrow(() -> new IllegalArgumentException("Card number is required"));
            Long month = runContext.render(this.expMonth).as(Long.class)
                .orElseThrow(() -> new IllegalArgumentException("Expiration month is required"));
            Long year = runContext.render(this.expYear).as(Long.class)
                .orElseThrow(() -> new IllegalArgumentException("Expiration year is required"));
            String cvc = runContext.render(this.cvc).as(String.class).orElse(null);

            builder.setCard(
                PaymentMethodCreateParams.CardDetails.builder()
                    .setNumber(number)
                    .setExpMonth(month)
                    .setExpYear(year)
                    .setCvc(cvc)
                    .build()
            );
        }

        // Use the client from AbstractStripe
        PaymentMethod paymentMethod = client(runContext).paymentMethods().create(builder.build());

        // Convert to Map for raw response
        Map<String, Object> paymentMethodMap = JacksonMapper.ofJson().readValue(
            paymentMethod.toJson(),
            new TypeReference<Map<String, Object>>() {}
        );

        return Output.builder()
            .paymentMethodId(paymentMethod.getId())
            .type(paymentMethod.getType())
            .rawResponse(paymentMethodMap)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the created PaymentMethod.")
        private final String paymentMethodId;

        @Schema(title = "The type of the PaymentMethod (e.g. `card`).")
        private final String type;

        @Schema(title = "The raw PaymentMethod object.")
        private final Map<String, Object> rawResponse;
    }
}