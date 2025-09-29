package io.kestra.plugin.stripe.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.PaymentMethodListParams;
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

import java.util.List;
import java.util.stream.Collectors;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List all PaymentMethods for a Customer",
    description = "This task retrieves all PaymentMethods (cards, bank accounts, etc.) attached to a given Stripe Customer."
)
@Plugin(
    examples = {
        @Example(
            title = "List all cards for a customer",
            full = true,
            code = """
                id: list_pms
                namespace: company.team

                tasks:
                  - id: list_pms
                    type: io.kestra.plugin.stripe.payment.ListPaymentMethods
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    customerId: cus_123
                    type: card
                """
        )
    }
)
public class ListPaymentMethods extends AbstractStripe implements RunnableTask<ListPaymentMethods.Output> {

    @Schema(title = "ID of the customer")
    @PluginProperty
    @NotNull
    private Property<String> customerId;

    @Schema(title = "Type of PaymentMethods to list (card, sepa_debit, etc.)")
    @PluginProperty
    @NotNull
    private Property<String> paymentMethodType;

    @PluginProperty
    private Property<String> apiKey;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Render API key
        String key = runContext.render(this.apiKey).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Stripe API key is required"));
        Stripe.apiKey = key;

        // Resolve properties
        String cusId = runContext.render(this.customerId).as(String.class).orElseThrow();
        String pmType = runContext.render(this.paymentMethodType).as(String.class).orElseThrow();

        try {
            PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(cusId)
                .setType(PaymentMethodListParams.Type.valueOf(pmType.toUpperCase()))
                .build();

            PaymentMethodCollection collection = PaymentMethod.list(params);

            List<String> pmIds = collection.getData().stream()
                .map(PaymentMethod::getId)
                .collect(Collectors.toList());

            return Output.builder()
                .customerId(cusId)
                .paymentMethodIds(pmIds)
                .raw(collection.toJson())
                .build();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to list PaymentMethods for customer: " + cusId, e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Customer ID")
        private final String customerId;

        @Schema(title = "List of PaymentMethod IDs")
        private final List<String> paymentMethodIds;

        @Schema(title = "Raw JSON returned by Stripe")
        private final String raw;
    }
}
