package com.mesutpiskin.keycloak.auth.email.service.impl;

import com.mesutpiskin.keycloak.auth.email.model.EmailMessage;
import com.mesutpiskin.keycloak.auth.email.service.EmailSender;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;

import java.io.IOException;

/**
 * SendGrid email sender implementation using SendGrid's REST API.
 * <p>
 * This implementation uses the SendGrid Java SDK to send emails through
 * SendGrid's email delivery service. Requires a valid SendGrid API key.
 * </p>
 * 
 * <p>
 * Configuration requirements:
 * <ul>
 * <li>SendGrid API Key (required)</li>
 * <li>From Email Address (required)</li>
 * </ul>
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.1.0
 */
public class SendGridEmailSender implements EmailSender {

    private static final Logger logger = Logger.getLogger(SendGridEmailSender.class);

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    /**
     * Constructs a new SendGridEmailSender.
     *
     * @param apiKey    the SendGrid API key
     * @param fromEmail the sender email address
     * @param fromName  the sender display name (optional)
     */
    public SendGridEmailSender(String apiKey, String fromEmail, String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName != null ? fromName : fromEmail;
    }

    @Override
    public void sendEmail(EmailMessage message) throws EmailException {
        if (!isAvailable()) {
            throw new EmailException("SendGrid is not properly configured");
        }

        try {
            SendGrid sendGrid = new SendGrid(apiKey);

            Email from = new Email(fromEmail, fromName);
            Email to = new Email(message.getTo());
            String subject = message.getSubject();

            // Build email content - prefer HTML over text
            Content content;
            if (message.getHtmlBody() != null && !message.getHtmlBody().isEmpty()) {
                content = new Content("text/html", message.getHtmlBody());
            } else if (message.getTextBody() != null && !message.getTextBody().isEmpty()) {
                content = new Content("text/plain", message.getTextBody());
            } else {
                // Fallback: build simple text from template data
                content = buildContentFromTemplateData(message);
            }

            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            // SendGrid returns 202 for successful queuing
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.debugf("Email sent successfully via SendGrid to %s (status: %d)",
                        message.getTo(), response.getStatusCode());
            } else {
                String errorMsg = String.format("SendGrid API returned error status %d: %s",
                        response.getStatusCode(), response.getBody());
                logger.error(errorMsg);
                throw new EmailException(errorMsg);
            }

        } catch (IOException e) {
            String errorMsg = String.format("Failed to send email via SendGrid to %s", message.getTo());
            logger.errorf(e, errorMsg);
            throw new EmailException(errorMsg, e);
        }
    }

    @Override
    public String getProviderName() {
        return "SendGrid";
    }

    @Override
    public boolean isAvailable() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("SendGrid API key is not configured");
            return false;
        }
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            logger.warn("SendGrid from email is not configured");
            return false;
        }
        return true;
    }

    /**
     * Builds email content from template data when no HTML or text body is
     * provided.
     * This is a fallback mechanism to support the existing template-based approach.
     *
     * @param message the email message containing template data
     * @return the constructed email content
     */
    private Content buildContentFromTemplateData(EmailMessage message) {
        StringBuilder textContent = new StringBuilder();

        Object username = message.getTemplateData().get("username");
        Object code = message.getTemplateData().get("code");
        Object ttl = message.getTemplateData().get("ttl");

        if (username != null) {
            textContent.append("Hello ").append(username).append(",\n\n");
        }

        if (code != null) {
            textContent.append("Your verification code is: ").append(code).append("\n\n");
        }

        if (ttl != null) {
            textContent.append("This code will expire in ").append(ttl).append(" minutes.\n\n");
        }

        textContent.append("If you did not request this code, please ignore this email.");

        return new Content("text/plain", textContent.toString());
    }
}
