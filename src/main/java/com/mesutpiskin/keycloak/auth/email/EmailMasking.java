package com.mesutpiskin.keycloak.auth.email;

import java.util.Map;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;

/**
 * Exposes a partially masked destination address to the OTP forms, so users
 * know which mailbox to check without the full address being disclosed.
 */
final class EmailMasking {

    private EmailMasking() {
    }

    static void applyToForm(LoginFormsProvider form, UserModel user, Map<String, String> configValues) {
        if (!Boolean.parseBoolean(configValues.getOrDefault(
                EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM,
                String.valueOf(EmailConstants.DEFAULT_SHOW_MASKED_EMAIL_ON_OTP_FORM)))) {
            return;
        }

        String maskedEmail = mask(user);
        if (maskedEmail != null) {
            form.setAttribute("maskedEmail", maskedEmail);
        }
    }

    private static String mask(UserModel user) {
        if (user == null) {
            return null;
        }

        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            return null;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return null;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domainPart;
        }

        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
    }
}
