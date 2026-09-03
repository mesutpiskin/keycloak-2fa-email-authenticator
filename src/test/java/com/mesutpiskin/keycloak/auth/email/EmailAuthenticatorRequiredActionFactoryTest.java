package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EmailAuthenticatorRequiredActionFactory Tests")
class EmailAuthenticatorRequiredActionFactoryTest {

    @Test
    @DisplayName("Exposes masked-email config on the required action")
    void exposesMaskedEmailConfig() {
        var factory = new EmailAuthenticatorRequiredActionFactory();

        var masked = factory.getConfigMetadata().stream()
                .filter(p -> EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM.equals(p.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(ProviderConfigProperty.BOOLEAN_TYPE, masked.getType());
        assertEquals(String.valueOf(EmailConstants.DEFAULT_SHOW_MASKED_EMAIL_ON_OTP_FORM),
                masked.getDefaultValue());
    }

    @Test
    @DisplayName("Exposes sending config keys on the required action")
    void exposesSendingConfigKeys() {
        var factory = new EmailAuthenticatorRequiredActionFactory();
        Set<String> names = factory.getConfigMetadata().stream()
                .map(ProviderConfigProperty::getName)
                .collect(Collectors.toSet());

        assertTrue(names.contains(EmailConstants.EMAIL_PROVIDER_TYPE));
        assertTrue(names.contains(EmailConstants.CODE_TTL));
        assertTrue(names.contains(EmailConstants.CODE_LENGTH));
        assertTrue(names.contains(EmailConstants.MAX_ATTEMPTS));
    }
}
