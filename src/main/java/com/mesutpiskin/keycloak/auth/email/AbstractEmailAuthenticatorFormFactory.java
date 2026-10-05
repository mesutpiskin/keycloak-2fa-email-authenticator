package com.mesutpiskin.keycloak.auth.email;

import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Base factory for every email OTP authenticator in this extension.
 * <p>
 * It holds what all variants share: the configuration properties read by
 * {@link AbstractEmailAuthenticatorForm} (email provider, transport credentials
 * and OTP policy), the requirement choices, and the lifecycle no-ops. A concrete
 * factory supplies only what tells it apart: its id, admin-console labels,
 * reference category, whether user setup is allowed, and the authenticator
 * instance it creates.
 * </p>
 * <p>
 * When adding a new variant, also add its id to {@link #PROVIDER_IDS} (and to
 * {@link #ENROLMENT_PROVIDER_IDS} if users enrol for it), so code that scans
 * realm flows for this extension's executions recognises it.
 * </p>
 *
 * @since 26.7.0
 */
public abstract class AbstractEmailAuthenticatorFormFactory implements AuthenticatorFactory {

    /**
     * Provider ids of the credential-based authenticators, the ones users enrol
     * for. Their configuration carries the enrolment settings read by
     * {@link EmailAuthenticatorRequiredAction}.
     */
    public static final Set<String> ENROLMENT_PROVIDER_IDS = Set.of(
            EmailAuthenticatorFormFactory.PROVIDER_ID,
            ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID);

    /**
     * Provider ids of every email OTP authenticator registered by this extension.
     */
    public static final Set<String> PROVIDER_IDS = Set.of(
            EmailAuthenticatorFormFactory.PROVIDER_ID,
            ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID,
            NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID);

    /**
     * Returns whether {@code providerId} belongs to one of this extension's email
     * OTP authenticators.
     *
     * @param providerId an authentication execution's authenticator id; may be
     *                   {@code null}
     * @return {@code true} if it is one of {@link #PROVIDER_IDS}
     */
    public static boolean isEmailAuthenticator(String providerId) {
        return providerId != null && PROVIDER_IDS.contains(providerId);
    }

    /**
     * Returns whether {@code providerId} belongs to one of the credential-based
     * authenticators (see {@link #ENROLMENT_PROVIDER_IDS}).
     *
     * @param providerId an authentication execution's authenticator id; may be
     *                   {@code null}
     * @return {@code true} if it is one of {@link #ENROLMENT_PROVIDER_IDS}
     */
    public static boolean isEnrolmentCapable(String providerId) {
        return providerId != null && ENROLMENT_PROVIDER_IDS.contains(providerId);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    /**
     * Returns the configuration shared by every email OTP authenticator.
     * Subclasses that need extra settings should append to this list, not
     * replace it.
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                // Email provider selection
                new ProviderConfigProperty(EmailConstants.EMAIL_PROVIDER_TYPE, "Email Provider",
                        "Select the email service provider to use for sending verification codes.",
                        ProviderConfigProperty.LIST_TYPE, EmailConstants.DEFAULT_EMAIL_PROVIDER,
                        List.of("KEYCLOAK", "SENDGRID", "AWS_SES", "MAILGUN").toArray(new String[0])),

                // SendGrid configuration
                new ProviderConfigProperty(EmailConstants.SENDGRID_API_KEY, "SendGrid API Key",
                        "SendGrid API key (required when Email Provider is set to SENDGRID).",
                        ProviderConfigProperty.PASSWORD, null),
                new ProviderConfigProperty(EmailConstants.SENDGRID_FROM_EMAIL, "SendGrid From Email",
                        "Sender email address for SendGrid (required when Email Provider is set to SENDGRID).",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.SENDGRID_FROM_NAME, "SendGrid From Name",
                        "Sender display name for SendGrid (optional, defaults to from email).",
                        ProviderConfigProperty.STRING_TYPE, null),

                // AWS SES configuration
                new ProviderConfigProperty(EmailConstants.AWS_SES_REGION, "AWS SES Region",
                        "AWS region code for SES (e.g., us-east-1, eu-west-1). Required when Email Provider is set to AWS_SES.",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.AWS_ACCESS_KEY_ID, "AWS Access Key ID",
                        "AWS IAM access key ID with SES permissions (required when Email Provider is set to AWS_SES).",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.AWS_SECRET_ACCESS_KEY, "AWS Secret Access Key",
                        "AWS IAM secret access key (required when Email Provider is set to AWS_SES).",
                        ProviderConfigProperty.PASSWORD, null),
                new ProviderConfigProperty(EmailConstants.AWS_SES_FROM_EMAIL, "AWS SES From Email",
                        "Verified sender email address for AWS SES (required when Email Provider is set to AWS_SES).",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.AWS_SES_FROM_NAME, "AWS SES From Name",
                        "Sender display name for AWS SES (optional, defaults to from email).",
                        ProviderConfigProperty.STRING_TYPE, null),

                // Mailgun configuration
                new ProviderConfigProperty(EmailConstants.MAILGUN_API_KEY, "Mailgun API Key",
                        "Mailgun API key (required when Email Provider is set to MAILGUN).",
                        ProviderConfigProperty.PASSWORD, null),
                new ProviderConfigProperty(EmailConstants.MAILGUN_DOMAIN, "Mailgun Domain",
                        "Mailgun sending domain, e.g. mg.example.com (required when Email Provider is set to MAILGUN).",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.MAILGUN_FROM_EMAIL, "Mailgun From Email",
                        "Sender email address for Mailgun (required when Email Provider is set to MAILGUN).",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.MAILGUN_FROM_NAME, "Mailgun From Name",
                        "Sender display name for Mailgun (optional, defaults to from email).",
                        ProviderConfigProperty.STRING_TYPE, null),
                new ProviderConfigProperty(EmailConstants.MAILGUN_REGION, "Mailgun Region",
                        "Mailgun API region: US (default) or EU. Use EU if your Mailgun account is on the EU region.",
                        ProviderConfigProperty.LIST_TYPE, EmailConstants.DEFAULT_MAILGUN_REGION,
                        new String[]{"US", "EU"}),

                new ProviderConfigProperty(EmailConstants.ENABLE_FALLBACK, "Enable Fallback to Keycloak SMTP",
                        "If enabled, falls back to Keycloak SMTP when the primary provider fails.",
                        ProviderConfigProperty.BOOLEAN_TYPE, String.valueOf(EmailConstants.DEFAULT_ENABLE_FALLBACK)),

                // Existing OTP configuration
                new ProviderConfigProperty(EmailConstants.CODE_LENGTH, "Code Length",
                        "The number of digits of the generated code.",
                        ProviderConfigProperty.STRING_TYPE, String.valueOf(EmailConstants.DEFAULT_LENGTH)),
                new ProviderConfigProperty(EmailConstants.CODE_TTL, "Time-to-Live (seconds)",
                        "The time to live in seconds for the code to be valid.",
                        ProviderConfigProperty.STRING_TYPE, String.valueOf(EmailConstants.DEFAULT_TTL)),
                new ProviderConfigProperty(EmailConstants.SIMULATION_MODE, "Simulation Mode (dev only)",
                        "In simulation mode, the mail won't be sent, but printed to the server logs",
                        ProviderConfigProperty.BOOLEAN_TYPE, String.valueOf(EmailConstants.DEFAULT_SIMULATION_MODE)),
                new ProviderConfigProperty(EmailConstants.RESEND_COOLDOWN, "Resend Cooldown (seconds)",
                        "The minimum number of seconds a user must wait before requesting a new code.",
                        ProviderConfigProperty.STRING_TYPE, String.valueOf(EmailConstants.DEFAULT_RESEND_COOLDOWN)),
                new ProviderConfigProperty(EmailConstants.MAX_ATTEMPTS, "Max Code Attempts",
                        "The maximum number of invalid code attempts before the code is invalidated and a new one must be requested.",
                        ProviderConfigProperty.STRING_TYPE, String.valueOf(EmailConstants.DEFAULT_MAX_ATTEMPTS)),
                new ProviderConfigProperty(EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM, "Show Masked Email on OTP Form",
                        "If enabled, displays a masked version of the user's email address on the OTP entry form after the code is sent, both at login and during enrolment.",
                        ProviderConfigProperty.BOOLEAN_TYPE, String.valueOf(EmailConstants.DEFAULT_SHOW_MASKED_EMAIL_ON_OTP_FORM)));
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }
}
