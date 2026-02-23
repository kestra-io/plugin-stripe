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
    title = "Validate and parse Stripe webhooks",
    description = "Validates Stripe webhook signatures with the endpoint secret, then returns the event id/type plus the deserialized data map. Fails fast on signature mismatch; provide the raw request body and the `Stripe-Signature` header as received."
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

                triggers:
                  - id: webhook
                    type: io.kestra.plugin.core.trigger.Webhook
                    key: 4wjtkzwVGBM9yKnjm3yv8r
                """
        )
    }
)
public class HandleEvent extends AbstractStripe implements RunnableTask<HandleEvent.Output> {

    @Schema(title = "Webhook payload body", description = "Raw JSON request body from Stripe; must be unmodified for signature verification")
    @NotNull
    private Property<String> payload;

    @Schema(title = "Stripe-Signature header value", description = "Exact `Stripe-Signature` header from the incoming request")
    @NotNull
    private Property<String> signatureHeader;

    @Schema(title = "Webhook endpoint secret", description = "Signing secret configured on the Stripe webhook endpoint; required for signature validation")
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

        @Schema(title = "Stripe event type", description = "Event type string such as `charge.succeeded`")
        private final String type;

        @Schema(title = "Event data payload", description = "Deserialized object map from `event.data.object`")
        private final Map<String, Object> data;

        @Schema(title = "Raw webhook payload", description = "Original payload body returned for downstream auditing")
        private final String raw;
    }
}
