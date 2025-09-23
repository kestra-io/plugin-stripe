package io.kestra.plugin.stripe;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.IOUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a new customer in Stripe.",
    description = "This task creates a customer in Stripe using the REST API."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a customer with email and name",
            full = true,
            code = """
                id: stripe_create_customer
                namespace: company.team

                tasks:
                  - id: create_customer
                    type: io.kestra.plugin.stripe.CreateCustomer
                    apiKey: "{{ secret('STRIPE_API_KEY') }}"
                    email: "john@example.com"
                    name: "John Doe"
                """
        )
    }
)
public class CreateCustomer extends AbstractStripe implements RunnableTask<CreateCustomer.Output> {

    @Schema(title = "Customer email")
    @NotNull
    private Property<String> email;

    @Schema(title = "Customer name")
    private Property<String> name;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (HttpClient client = this.client(runContext)) {
            String renderedEmail = runContext.render(this.email).as(String.class).orElseThrow();
            String renderedName = runContext.render(this.name).as(String.class).orElse(null);

            StringBuilder body = new StringBuilder("email=" + renderedEmail);
            if (renderedName != null) {
                body.append("&name=").append(renderedName);
            }

            String endpoint = buildResourceEndpoint("customers");

            HttpRequest request = baseRequest(runContext, endpoint)
                .method("POST")
                .body(HttpRequest.StringRequestBody.builder()
                    .content(body.toString())
                    .contentType("application/x-www-form-urlencoded")
                    .build())
                .build();

            HttpResponse<byte[]> response = client.request(request, byte[].class);

            String responseBody = null;
            if (response.getBody() != null) {
                responseBody = IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
            }

            return Output.builder()
                .uri(request.getUri())
                .code(response.getStatus().getCode())
                .headers(response.getHeaders().map())
                .rawResponse(responseBody)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "The URI of the executed request.")
        private final URI uri;

        @Schema(title = "The HTTP status code of the response.")
        private final Integer code;

        @Schema(title = "The headers of the response.")
        private final Map<String, List<String>> headers;

        @Schema(title = "The raw response body from Stripe.")
        private final String rawResponse;
    }
}
