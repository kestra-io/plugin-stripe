package io.kestra.plugin.stripe.customer;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.validations.ModelValidator;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class ListValidationTest {
    @Inject
    private ModelValidator modelValidator;

    @Test
    void limit() {
        assertThat(modelValidator.isValid(task(10)).isEmpty(), is(true));
        assertThat(modelValidator.isValid(task(0)).isPresent(), is(true));
    }

    private static List task(int limit) {
        return List.builder()
            .id("list")
            .type(List.class.getName())
            .apiKey(Property.ofValue("sk_test_validation"))
            .limit(Property.ofValue(limit))
            .build();
    }
}
