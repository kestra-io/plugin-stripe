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
    title = "Detach a PaymentMethod from a Customer",
    description = "This task removes an existing PaymentMethod from a Stripe Customer."
)
@Plugin(
    examples = {
        @Example(
            title = "Detach a card from a customer",
            full = true,
            code = """
                id: detach_pm
                namespace: company.team

                tasks:
                  - id: detach_pm
                    type: io.kestra.plugin.stripe.payment.DetachPaymentMethod
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentMethodId: pm_123
                """
        )
    }
)
public class DetachPaymentMethod extends AbstractStripe implements RunnableTask<DetachPaymentMethod.Output> {

    @Schema(title = "ID of the PaymentMethod to detach")
    @NotNull
    private Property<String> paymentMethodId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String apiKey = renderApiKey(runContext);
        Stripe.apiKey = apiKey;

        String pmId = runContext.render(paymentMethodId).as(String.class).orElseThrow();

        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(pmId);
            PaymentMethod detached = paymentMethod.detach();

            return Output.builder()
                .id(detached.getId())
                .customer(detached.getCustomer())
                .type(detached.getType())
                .raw(detached.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to detach PaymentMethod from customer", e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "ID of the detached PaymentMethod")
        private final String id;

        @Schema(title = "ID of the customer (null after detachment)")
        private final String customer;

        @Schema(title = "Type of the PaymentMethod")
        private final String type;

        @Schema(title = "Raw JSON returned by Stripe")
        private final String raw;
    }
}
