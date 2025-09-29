package io.kestra.plugin.stripe.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodAttachParams;
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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Attach a PaymentMethod to a Customer.",
    description = "This task attaches an existing PaymentMethod to a Stripe Customer and returns the attached PaymentMethod object."
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

    @Schema(title = "The ID of the PaymentMethod to attach.")
    @NotNull
    private Property<String> paymentMethodId;

    @Schema(title = "The ID of the Customer to attach the PaymentMethod to.")
    @NotNull
    private Property<String> customerId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve parameters
        String pmId = runContext.render(this.paymentMethodId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("PaymentMethod ID is required"));

        String cusId = runContext.render(this.customerId)
            .as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Customer ID is required"));

        PaymentMethod attached;
        try {
            // Use the client from AbstractStripe
            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(cusId)
                .build();

            attached = client(runContext).paymentMethods().attach(pmId, params);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to attach PaymentMethod to customer: " + e.getMessage(), e);
        }

        // Convert Stripe PaymentMethod JSON to Map<String,Object>
        String json = attached.getLastResponse().body();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> paymentMethodData = mapper.readValue(json, new TypeReference<>() {});

        return Output.builder()
            .paymentMethodId(attached.getId())
            .customerId(attached.getCustomer())
            .type(attached.getType())
            .paymentMethodData(paymentMethodData)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The ID of the attached PaymentMethod.")
        private final String paymentMethodId;

        @Schema(title = "The ID of the Customer the PaymentMethod is attached to.")
        private final String customerId;

        @Schema(title = "The type of the PaymentMethod (e.g., card, bank_account).")
        private final String type;

        @Schema(title = "The full PaymentMethod object as a map.")
        @PluginProperty
        private final Map<String, Object> paymentMethodData;
    }
}