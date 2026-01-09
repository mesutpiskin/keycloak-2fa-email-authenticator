package com.mesutpiskin.keycloak.auth.email.model;

/**
 * Enumeration of supported email provider types.
 * <p>
 * This enum defines all available email service providers that can be used
 * for sending verification codes. Each provider has a display name used in
 * the Keycloak admin UI configuration.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.1.0
 */
public enum EmailProviderType {

    /**
     * Default Keycloak email provider using SMTP configuration from Realm Settings.
     * This is the backward-compatible option that uses Keycloak's built-in email
     * functionality.
     */
    KEYCLOAK("Keycloak SMTP"),

    /**
     * SendGrid email service provider using SendGrid's REST API.
     * Requires SendGrid API key configuration.
     */
    SENDGRID("SendGrid"),

    /**
     * Amazon Web Services Simple Email Service (SES).
     * Requires AWS credentials and region configuration.
     */
    AWS_SES("AWS SES"),

    /**
     * Mailgun email service provider using Mailgun's REST API.
     * Requires Mailgun API key and domain configuration.
     */
    MAILGUN("Mailgun");

    private final String displayName;

    EmailProviderType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this provider type.
     *
     * @return the display name used in UI and logs
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string value to the corresponding EmailProviderType.
     * <p>
     * If the value is null, empty, or doesn't match any provider,
     * returns KEYCLOAK as the default.
     * </p>
     *
     * @param value the provider type string
     * @return the matching EmailProviderType or KEYCLOAK as default
     */
    public static EmailProviderType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return KEYCLOAK;
        }

        try {
            return EmailProviderType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KEYCLOAK;
        }
    }
}
