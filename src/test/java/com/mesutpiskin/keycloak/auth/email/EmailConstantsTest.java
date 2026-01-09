package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EmailConstants} to verify constant values and utility
 * class pattern.
 */
@DisplayName("EmailConstants Tests")
class EmailConstantsTest {

    @Test
    @DisplayName("Should have correct default code length")
    void testDefaultCodeLength() {
        assertEquals(6, EmailConstants.DEFAULT_LENGTH,
                "Default code length should be 6 digits");
    }

    @Test
    @DisplayName("Should have correct default TTL")
    void testDefaultTTL() {
        assertEquals(300, EmailConstants.DEFAULT_TTL,
                "Default TTL should be 300 seconds (5 minutes)");
    }

    @Test
    @DisplayName("Should have correct default simulation mode")
    void testDefaultSimulationMode() {
        assertFalse(EmailConstants.DEFAULT_SIMULATION_MODE,
                "Simulation mode should be disabled by default");
    }

    @Test
    @DisplayName("Should have correct default resend cooldown")
    void testDefaultResendCooldown() {
        assertEquals(30, EmailConstants.DEFAULT_RESEND_COOLDOWN,
                "Default resend cooldown should be 30 seconds");
    }

    @Test
    @DisplayName("Should have correct millisecond rounding offset")
    void testMillisRoundingOffset() {
        assertEquals(999L, EmailConstants.MILLIS_ROUNDING_OFFSET,
                "Millisecond rounding offset should be 999L for ceiling division");
    }

    @Test
    @DisplayName("Should not be instantiable")
    void testUtilityClassPattern() {
        try {
            var constructor = EmailConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
                constructor.newInstance();
            }, "EmailConstants constructor should throw when invoked");

            // Verify the cause is UnsupportedOperationException
            assertTrue(exception.getCause() instanceof UnsupportedOperationException,
                    "Cause should be UnsupportedOperationException");
            assertEquals("EmailConstants is a utility class and cannot be instantiated",
                    exception.getCause().getMessage());
        } catch (NoSuchMethodException e) {
            fail("Private constructor should exist");
        }
    }

    @Test
    @DisplayName("Should have correct constant string values")
    void testConstantStringValues() {
        assertEquals("emailCode", EmailConstants.CODE);
        assertEquals("length", EmailConstants.CODE_LENGTH);
        assertEquals("ttl", EmailConstants.CODE_TTL);
        assertEquals("simulationMode", EmailConstants.SIMULATION_MODE);
        assertEquals("resendCooldown", EmailConstants.RESEND_COOLDOWN);
        assertEquals("emailCodeResendAvailableAfter", EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
    }
}
