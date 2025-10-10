package io.kestra.plugin.stripe.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import io.kestra.core.serializers.JacksonMapper;
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
    title = "Handle Stripe Webhook Events.",
    description = "This task receives a Stripe webhook payload, validates the signature, and outputs the parsed event."
)
@Plugin(
    examples = {
        @Example(
            title = "Receive a Stripe webhook",
            full = true,
            code = """
                id: stripe_webhook
                namespace: company.team

                tasks:
                  - id: handle_webhook
                    type: io.kestra.plugin.stripe.webhook.HandleEvent
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    payload: "{{ trigger.payload }}"
                    signatureHeader: "{{ trigger.headers['Stripe-Signature'] }}"
                    endpointSecret: "{{ secret('STRIPE_WEBHOOK_SECRET') }}"
                """
        )
    }
)
public class HandleEvent extends AbstractStripe implements RunnableTask<HandleEvent.Output> {

    @Schema(title = "Raw webhook payload from Stripe")
    @NotNull
    private Property<String> payload;

    @Schema(title = "Stripe-Signature header")
    @NotNull
    private Property<String> signatureHeader;

    @Schema(title = "Endpoint secret configured in Stripe for webhook validation")
    @NotNull
    private Property<String> endpointSecret;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String rawPayload = runContext.render(payload).as(String.class).orElseThrow();
        String sigHeader = runContext.render(signatureHeader).as(String.class).orElseThrow();
        String secret = runContext.render(endpointSecret).as(String.class).orElseThrow();

        try {
            Event event = Webhook.constructEvent(rawPayload, sigHeader, secret);
            StripeObject stripeObject = event.getData().getObject();

            // Convert StripeObject to Map
            Map<String, Object> dataMap = JacksonMapper.ofJson().convertValue(stripeObject, Map.class);

            return Output.builder()
                .id(event.getId())
                .type(event.getType())
                .data(dataMap)
                .raw(rawPayload)
                .build();
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid Stripe webhook signature", e);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Stripe event ID")
        private final String id;

        @Schema(title = "Stripe event Type")
        private final String type;

        @Schema(title = "Event data object")
        private final Map<String, Object> data;

        @Schema(title = "Raw payload of the webhook")
        private final String raw;
    }
}
