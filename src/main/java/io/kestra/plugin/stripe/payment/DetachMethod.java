package io.kestra.plugin.stripe.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.serializers.JacksonMapper;
import com.stripe.model.PaymentMethod;
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

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Detach PaymentMethod from customer",
    description = "Detaches an existing PaymentMethod by ID. After detachment the customer field becomes null. Returns the raw PaymentMethod payload."
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
                    type: io.kestra.plugin.stripe.payment.DetachMethod
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    paymentMethodId: pm_123
                """
        )
    }
)
public class DetachMethod extends AbstractStripe implements RunnableTask<DetachMethod.Output> {

    @NotNull
    @Schema(title = "PaymentMethod ID to detach")
    private Property<String> paymentMethodId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        // Resolve PaymentMethod ID
        String pmId = runContext.render(this.paymentMethodId).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("PaymentMethod ID is required"));

        // Use the client from AbstractStripe
        PaymentMethod detached = client(runContext).paymentMethods().detach(pmId);

        // Convert to Map
        Map<String, Object> detachedMap = JacksonMapper.ofJson().readValue(
            detached.toJson(),
            new TypeReference<>() {}
        );

        return Output.builder()
            .id(detached.getId())
            .customer(detached.getCustomer())
            .type(detached.getType())
            .rawResponse(detachedMap)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Detached PaymentMethod ID")
        private final String id;

        @Schema(title = "Customer ID before detach", description = "Null when no customer remains attached")
        private final String customer;

        @Schema(title = "PaymentMethod type")
        private final String type;

        @Schema(title = "Raw PaymentMethod payload", description = "Full PaymentMethod object after detach")
        private final Map<String, Object> rawResponse;
    }
}
