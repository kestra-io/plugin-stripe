package io.kestra.plugin.stripe.customer;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.kestra.core.models.property.Property;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Regression test for the HV000030 crash: bean validation must be able to
 * validate a {@link List} task. Before the fix, the unsupported {@code @Min}
 * constraint on the {@code Property<Integer>} limit field made
 * {@link Validator#validate(Object)} throw an UnexpectedTypeException
 * (HV000030) instead of returning violations.
 *
 * <p>This test needs no Stripe API key, unlike {@link ListTest}.</p>
 */
class ListValidationTest {

    @Test
    void limitPropertyCanBeValidated() {
        List task = List.builder()
            .apiKey(Property.ofValue("sk_test_validation"))
            .limit(Property.ofValue(10))
            .build();

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<List>> violations = validator.validate(task);

        assertThat(violations, is(empty()));
    }
}
