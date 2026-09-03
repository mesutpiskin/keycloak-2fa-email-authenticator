package com.mesutpiskin.keycloak.auth.email;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EmailAuthenticatorRequiredAction Tests")
class EmailAuthenticatorRequiredActionTest {

    @Test
    @DisplayName("hasSendingConfig returns false for empty map")
    void hasSendingConfigEmpty() {
        assertFalse(EmailAuthenticatorRequiredAction.hasSendingConfig(Map.of()));
    }

    @Test
    @DisplayName("hasSendingConfig ignores max_auth_age alone")
    void hasSendingConfigIgnoresMaxAuthAge() {
        assertFalse(EmailAuthenticatorRequiredAction.hasSendingConfig(Map.of("max_auth_age", "300")));
    }

    @Test
    @DisplayName("hasSendingConfig ignores masked-email toggle alone")
    void hasSendingConfigIgnoresMaskedEmail() {
        assertFalse(EmailAuthenticatorRequiredAction.hasSendingConfig(
                Map.of(EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM, "true")));
    }

    @Test
    @DisplayName("hasSendingConfig ignores blank sending values")
    void hasSendingConfigIgnoresBlankValues() {
        assertFalse(EmailAuthenticatorRequiredAction.hasSendingConfig(
                Map.of(EmailConstants.CODE_TTL, "   ")));
    }

    @Test
    @DisplayName("hasSendingConfig returns true when a sending key is set")
    void hasSendingConfigDetectsSendingKey() {
        assertTrue(EmailAuthenticatorRequiredAction.hasSendingConfig(
                Map.of(EmailConstants.CODE_TTL, "300")));
    }
}
