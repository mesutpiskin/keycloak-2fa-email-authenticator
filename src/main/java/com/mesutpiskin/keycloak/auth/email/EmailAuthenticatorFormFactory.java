package com.mesutpiskin.keycloak.auth.email;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class EmailAuthenticatorFormFactory implements AuthenticatorFactory {

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
    public String getReferenceCategory() {
        return EmailAuthenticatorCredentialModel.TYPE_ID;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Email otp authenticator.";
    }

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
                        "If enabled, displays a masked version of the user's email address on the OTP entry form after the code is sent.",
                        ProviderConfigProperty.BOOLEAN_TYPE, String.valueOf(EmailConstants.DEFAULT_SHOW_MASKED_EMAIL_ON_OTP_FORM)),
                new ProviderConfigProperty(EmailConstants.SKIP_SETUP, "Treat any user with an email as configured",
                        "When enabled, users with an email address are reported as configured for the email authenticator even if they have never enrolled — useful for admin-provisioned 2FA and for showing the plugin in 'Try Another Way' alternatives. Leave disabled (default) when you rely on 'Conditional - User Configured' sub-flows: the conditional only triggers for users who have actually enrolled.",
                        ProviderConfigProperty.BOOLEAN_TYPE, String.valueOf(EmailConstants.DEFAULT_SKIP_SETUP)));
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
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
