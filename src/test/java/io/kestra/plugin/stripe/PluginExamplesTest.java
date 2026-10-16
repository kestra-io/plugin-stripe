package io.kestra.plugin.stripe;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.serializers.YamlParser;
import io.kestra.plugin.stripe.payment.CreateMethod;
import io.kestra.plugin.stripe.payment.ListMethods;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PluginExamplesTest {
    private static final List<Class<?>> PLUGIN_CLASSES = List.of(
        io.kestra.plugin.stripe.balance.Retrieve.class,
        io.kestra.plugin.stripe.customer.Create.class,
        io.kestra.plugin.stripe.customer.Delete.class,
        io.kestra.plugin.stripe.customer.Get.class,
        io.kestra.plugin.stripe.customer.List.class,
        io.kestra.plugin.stripe.customer.Update.class,
        io.kestra.plugin.stripe.payment.AttachMethod.class,
        io.kestra.plugin.stripe.payment.ConfirmIntent.class,
        io.kestra.plugin.stripe.payment.CreateIntent.class,
        CreateMethod.class,
        io.kestra.plugin.stripe.payment.DetachMethod.class,
        io.kestra.plugin.stripe.payment.ListIntents.class,
        ListMethods.class,
        io.kestra.plugin.stripe.payment.Refund.class,
        io.kestra.plugin.stripe.webhook.HandleEvent.class
    );

    @Test
    void fullExamplesAreValidFlows() {
        for (Class<?> pluginClass : PLUGIN_CLASSES) {
            for (Example example : pluginClass.getAnnotation(Plugin.class).examples()) {
                if (!example.full()) {
                    continue;
                }

                assertDoesNotThrow(
                    () -> YamlParser.parse(String.join("\n", example.code()), Flow.class),
                    pluginClass.getSimpleName() + " example \"" + example.title() + "\" is not a valid flow"
                );
            }
        }
    }

    @Test
    void createMethodExampleSetsPaymentMethodType() {
        CreateMethod task = (CreateMethod) firstTask(CreateMethod.class);

        assertThat(task.getPaymentMethodType(), is(notNullValue()));
    }

    @Test
    void listMethodsExampleSetsPaymentMethodType() {
        ListMethods task = (ListMethods) firstTask(ListMethods.class);

        assertThat(task.getPaymentMethodType(), is(notNullValue()));
    }

    private static Object firstTask(Class<?> pluginClass) {
        Example example = pluginClass.getAnnotation(Plugin.class).examples()[0];

        return YamlParser.parse(String.join("\n", example.code()), Flow.class).getTasks().getFirst();
    }
}
