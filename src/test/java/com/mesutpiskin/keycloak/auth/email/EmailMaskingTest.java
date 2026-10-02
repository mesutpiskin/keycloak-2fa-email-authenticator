package com.mesutpiskin.keycloak.auth.email;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmailMasking Tests")
class EmailMaskingTest {

    private static final Map<String, String> ENABLED = Map.of(EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM, "true");

    private final LoginFormsProvider form = mock(LoginFormsProvider.class);
    private final UserModel user = mock(UserModel.class);

    @Test
    @DisplayName("Masks the local part when enabled")
    void masksLocalPart() {
        when(user.getEmail()).thenReturn("username@example.com");

        EmailMasking.applyToForm(form, user, ENABLED);

        verify(form).setAttribute("maskedEmail", "u***e@example.com");
    }

    @Test
    @DisplayName("Masks short local parts without leaking the last character")
    void masksShortLocalPart() {
        when(user.getEmail()).thenReturn("ab@example.com");

        EmailMasking.applyToForm(form, user, ENABLED);

        verify(form).setAttribute("maskedEmail", "a***@example.com");
    }

    @Test
    @DisplayName("Exposes nothing when the user has no email")
    void skipsWhenNoEmail() {
        when(user.getEmail()).thenReturn(null);

        EmailMasking.applyToForm(form, user, ENABLED);

        verify(form, never()).setAttribute(eq("maskedEmail"), any());
    }

    @Test
    @DisplayName("Exposes nothing for malformed email addresses")
    void skipsWhenEmailMalformed() {
        when(user.getEmail()).thenReturn("user@");
        EmailMasking.applyToForm(form, user, ENABLED);
        verify(form, never()).setAttribute(eq("maskedEmail"), any());

        when(user.getEmail()).thenReturn("@example.com");
        EmailMasking.applyToForm(form, user, ENABLED);
        verify(form, never()).setAttribute(eq("maskedEmail"), any());

        when(user.getEmail()).thenReturn("not-an-email");
        EmailMasking.applyToForm(form, user, ENABLED);
        verify(form, never()).setAttribute(eq("maskedEmail"), any());
    }

    @Test
    @DisplayName("Exposes nothing when disabled by default")
    void skipsWhenDisabledByDefault() {
        when(user.getEmail()).thenReturn("username@example.com");

        EmailMasking.applyToForm(form, user, Map.of());

        verify(form, never()).setAttribute(eq("maskedEmail"), any());
    }
}
