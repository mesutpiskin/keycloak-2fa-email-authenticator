package com.mesutpiskin.keycloak.auth.email.service.impl;

import com.mesutpiskin.keycloak.auth.email.model.EmailMessage;
import com.mesutpiskin.keycloak.auth.email.service.EmailSender;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * AWS SES email sender implementation using AWS SDK v2.
 * <p>
 * This implementation uses Amazon Simple Email Service (SES) to send emails
 * through AWS's email delivery infrastructure. Requires AWS credentials and
 * a verified sender email address or domain.
 * </p>
 * 
 * <p>
 * Configuration requirements:
 * <ul>
 * <li>AWS Region (e.g., us-east-1, eu-west-1)</li>
 * <li>AWS Access Key ID</li>
 * <li>AWS Secret Access Key</li>
 * <li>Verified sender email address</li>
 * </ul>
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.1.0
 */
public class AwsSesEmailSender implements EmailSender {

    private static final Logger logger = Logger.getLogger(AwsSesEmailSender.class);

    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String fromEmail;
    private final String fromName;

    /**
     * Constructs a new AwsSesEmailSender.
     *
     * @param region          AWS region (e.g., "us-east-1")
     * @param accessKeyId     AWS access key ID
     * @param secretAccessKey AWS secret access key
     * @param fromEmail       verified sender email address
     * @param fromName        sender display name (optional)
     */
    public AwsSesEmailSender(String region, String accessKeyId, String secretAccessKey,
            String fromEmail, String fromName) {
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName != null ? fromName : fromEmail;
    }

    @Override
    public void sendEmail(EmailMessage message) throws EmailException {
        if (!isAvailable()) {
            throw new EmailException("AWS SES is not properly configured");
        }

        try {
            // Build AWS credentials
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);

            // Create SES client
            try (SesClient sesClient = SesClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build()) {

                // Build email content
                Content subject = Content.builder()
                        .data(message.getSubject())
                        .build();

                Content bodyContent;
                if (message.getHtmlBody() != null && !message.getHtmlBody().isEmpty()) {
                    bodyContent = Content.builder()
                            .data(message.getHtmlBody())
                            .build();
                } else if (message.getTextBody() != null && !message.getTextBody().isEmpty()) {
                    bodyContent = Content.builder()
                            .data(message.getTextBody())
                            .build();
                } else {
                    // Build from template data
                    bodyContent = Content.builder()
                            .data(buildTextFromTemplateData(message))
                            .build();
                }

                // Build message body
                Body body = Body.builder()
                        .text(bodyContent)
                        .build();

                Message sesMessage = Message.builder()
                        .subject(subject)
                        .body(body)
                        .build();

                // Build destination
                Destination destination = Destination.builder()
                        .toAddresses(message.getTo())
                        .build();

                // Build send request
                SendEmailRequest emailRequest = SendEmailRequest.builder()
                        .destination(destination)
                        .message(sesMessage)
                        .source(fromName.equals(fromEmail) ? fromEmail : fromName + " <" + fromEmail + ">")
                        .build();

                // Send email
                SendEmailResponse response = sesClient.sendEmail(emailRequest);

                logger.debugf("Email sent successfully via AWS SES to %s (MessageId: %s)",
                        message.getTo(), response.messageId());
            }

        } catch (SesException e) {
            String errorMsg = String.format("AWS SES error sending email to %s: %s",
                    message.getTo(), e.awsErrorDetails().errorMessage());
            logger.errorf(e, errorMsg);
            throw new EmailException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to send email via AWS SES to %s", message.getTo());
            logger.errorf(e, errorMsg);
            throw new EmailException(errorMsg, e);
        }
    }

    @Override
    public String getProviderName() {
        return "AWS SES";
    }

    @Override
    public boolean isAvailable() {
        if (region == null || region.trim().isEmpty()) {
            logger.warn("AWS SES region is not configured");
            return false;
        }
        if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
            logger.warn("AWS Access Key ID is not configured");
            return false;
        }
        if (secretAccessKey == null || secretAccessKey.trim().isEmpty()) {
            logger.warn("AWS Secret Access Key is not configured");
            return false;
        }
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            logger.warn("AWS SES from email is not configured");
            return false;
        }
        return true;
    }

    /**
     * Builds email text content from template data when no HTML or text body is
     * provided.
     *
     * @param message the email message containing template data
     * @return the constructed email text
     */
    private String buildTextFromTemplateData(EmailMessage message) {
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

        return textContent.toString();
    }
}
