package com.mesutpiskin.keycloak.auth.email.service;

import com.mesutpiskin.keycloak.auth.email.model.EmailMessage;
import org.keycloak.email.EmailException;

/**
 * Email sender abstraction interface for supporting multiple email providers.
 * <p>
 * This interface follows the Strategy Pattern to allow different email provider
 * implementations (SMTP, SendGrid, AWS SES, Mailgun, etc.) to be used
 * interchangeably.
 * </p>
 * 
 * <p>
 * Implementations should handle the specific details of connecting to and
 * sending
 * emails through their respective email service providers.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.1.0
 */
public interface EmailSender {

    /**
     * Sends an email message using the configured email provider.
     *
     * @param message the email message to send, containing recipient, subject, and
     *                body
     * @throws EmailException if the email cannot be sent due to network issues,
     *                        authentication failures, or provider-specific errors
     */
    void sendEmail(EmailMessage message) throws EmailException;

    /**
     * Returns the name of the email provider implementation.
     * <p>
     * This is useful for logging and debugging purposes to identify which
     * provider is being used for email delivery.
     * </p>
     *
     * @return the provider name (e.g., "Keycloak SMTP", "SendGrid", "AWS SES")
     */
    String getProviderName();

    /**
     * Checks if this email sender is properly configured and available for use.
     * <p>
     * Implementations should verify that required configuration parameters
     * (API keys, credentials, endpoints) are present and valid.
     * </p>
     *
     * @return true if the email sender is ready to send emails, false otherwise
     */
    boolean isAvailable();
}
