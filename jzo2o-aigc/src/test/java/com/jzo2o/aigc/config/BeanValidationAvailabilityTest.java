package com.jzo2o.aigc.config;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BeanValidationAvailabilityTest {

    @Test
    void shouldProvideBeanValidationImplementationAtRuntime() {
        assertDoesNotThrow(() -> Validation.buildDefaultValidatorFactory().close());
    }
}
