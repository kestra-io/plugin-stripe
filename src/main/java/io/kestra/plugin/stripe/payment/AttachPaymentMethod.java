package io.kestra.plugin.stripe.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Attach a PaymentMethod to a Customer",
    description = "This task links an existing PaymentMethod to a Stripe Customer."
)
@Plugin(
    examples = {
        @Example(
            title = "Attach a card to a customer",
            full = true,
            code = """
                id: attach_pm
                namespace: company.team

                tasks:
                  - id: attach_pm
                    type: io.kestra.plugin.stripe.payment.AttachPaymentMethod
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentMethodId: pm_123
                    customerId: cus_456
                """
        )
    }
)
public class AttachPaymentMethod extends AbstractStripe implements RunnableTask<AttachPaymentMethod.Output> {

    @Schema(title = "ID of the PaymentMethod to attach")
    @NotNull
    private Property<String> paymentMethodId;

    @Schema(title = "ID of the Customer to attach the PaymentMethod to")
    @NotNull
    private Property<String> customerId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String apiKey = renderApiKey(runContext);
        Stripe.apiKey = apiKey;

        String pmId = runContext.render(paymentMethodId).as(String.class).orElseThrow();
        String cusId = runContext.render(customerId).as(String.class).orElseThrow();

        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(pmId);
            PaymentMethod attached = paymentMethod.attach(
                Map.of("customer", cusId)
            );

            return Output.builder()
                .id(attached.getId())
                .customer(attached.getCustomer())
                .type(attached.getType())
                .raw(attached.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to attach PaymentMethod to customer", e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "ID of the attached PaymentMethod")
        private final String id;

        @Schema(title = "ID of the customer the PaymentMethod is attached to")
        private final String customer;

        @Schema(title = "Type of the PaymentMethod")
        private final String type;

        @Schema(title = "Raw JSON returned by Stripe")
        private final String raw;
    }
}
