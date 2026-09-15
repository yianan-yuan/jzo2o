package com.jzo2o.customer.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluationContactResolverTest {

    @Test
    void usesOrderContactPhoneWhenCustomerProfilePhoneIsBlank() {
        assertEquals("13333333333", EvaluationContactResolver.resolvePhone("", "13333333333"));
    }

    @Test
    void keepsCustomerProfilePhoneWhenItIsAvailable() {
        assertEquals("15555555555", EvaluationContactResolver.resolvePhone("15555555555", "13333333333"));
    }
}
