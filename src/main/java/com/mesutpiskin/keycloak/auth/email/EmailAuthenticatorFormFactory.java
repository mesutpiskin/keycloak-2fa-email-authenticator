package com.mesutpiskin.keycloak.auth.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Factory for {@link EmailAuthenticatorForm} ("Email OTP"), the
 * credential-based authenticator whose users must enrol first.
 * <p>
 * Besides the shared settings from {@link AbstractEmailAuthenticatorFormFactory},
 * it exposes the settings that only make sense when enrolment exists (currently
 * {@link EmailConstants#AUTO_ENROL_IF_EMAIL_VERIFIED}). The enrolment required
 * action reads those settings from this execution's configuration.
 * </p>
 *
 * @see NoEnrollmentEmailAuthenticatorFormFactory
 */
public class EmailAuthenticatorFormFactory extends AbstractEmailAuthenticatorFormFactory {

    public static final String PROVIDER_ID = "email-authenticator";
    public static final EmailAuthenticatorForm SINGLETON = new EmailAuthenticatorForm();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Email OTP";
    }

    @Override
    public String getHelpText() {
        return "Email OTP for users who have enrolled the email authenticator credential. "
                + "To offer email OTP to every user with an email address, use 'Email OTP (No Enrollment)'.";
    }

    /**
     * Links this authenticator to the {@code email-authenticator} credential type.
     * The account console uses this to offer enrolment, and Keycloak uses it to
     * pair the execution with the user's stored credentials.
     */
    @Override
    public String getReferenceCategory() {
        return EmailAuthenticatorCredentialModel.TYPE_ID;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>(super.getConfigProperties());
        properties.add(new ProviderConfigProperty(EmailConstants.AUTO_ENROL_IF_EMAIL_VERIFIED,
                "Auto-enrol users whose email is already verified",
                "When enabled, the 'email-authenticator-setup' required action enrols users whose email is already verified silently — the credential is created without sending or asking for a setup code, since Keycloak has already proven the user controls the mailbox. Users with an unverified email still go through the normal code-verification flow. Only enable this if you trust how 'emailVerified' is set in your realm (admin-provisioned accounts, imports or IdP mappers can set it without a real verification).",
                ProviderConfigProperty.BOOLEAN_TYPE, String.valueOf(EmailConstants.DEFAULT_AUTO_ENROL_IF_EMAIL_VERIFIED)));
        return Collections.unmodifiableList(properties);
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }
}
