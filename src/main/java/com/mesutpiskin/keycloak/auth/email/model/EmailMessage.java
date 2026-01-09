package com.mesutpiskin.keycloak.auth.email.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable email message model containing all information needed to send an
 * email.
 * <p>
 * This class uses the Builder pattern for flexible construction and ensures
 * immutability for thread safety.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.1.0
 */
public final class EmailMessage {

    private final String to;
    private final String from;
    private final String subject;
    private final String htmlBody;
    private final String textBody;
    private final Map<String, Object> templateData;

    private EmailMessage(Builder builder) {
        this.to = Objects.requireNonNull(builder.to, "Recipient email cannot be null");
        this.from = builder.from;
        this.subject = Objects.requireNonNull(builder.subject, "Email subject cannot be null");
        this.htmlBody = builder.htmlBody;
        this.textBody = builder.textBody;
        this.templateData = builder.templateData != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.templateData))
                : Collections.emptyMap();
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public Map<String, Object> getTemplateData() {
        return templateData;
    }

    /**
     * Creates a new builder instance for constructing EmailMessage objects.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing EmailMessage instances.
     */
    public static final class Builder {
        private String to;
        private String from;
        private String subject;
        private String htmlBody;
        private String textBody;
        private Map<String, Object> templateData;

        private Builder() {
        }

        /**
         * Sets the recipient email address.
         *
         * @param to the recipient email address (required)
         * @return this builder instance
         */
        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Sets the sender email address.
         *
         * @param from the sender email address (optional, may use provider default)
         * @return this builder instance
         */
        public Builder from(String from) {
            this.from = from;
            return this;
        }

        /**
         * Sets the email subject line.
         *
         * @param subject the email subject (required)
         * @return this builder instance
         */
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the HTML body content of the email.
         *
         * @param htmlBody the HTML email body (optional)
         * @return this builder instance
         */
        public Builder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        /**
         * Sets the plain text body content of the email.
         *
         * @param textBody the plain text email body (optional)
         * @return this builder instance
         */
        public Builder textBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        /**
         * Sets template data for email templates (e.g., FreeMarker variables).
         *
         * @param templateData map of template variable names to values
         * @return this builder instance
         */
        public Builder templateData(Map<String, Object> templateData) {
            this.templateData = templateData;
            return this;
        }

        /**
         * Builds and returns an immutable EmailMessage instance.
         *
         * @return a new EmailMessage instance
         * @throws NullPointerException if required fields (to, subject) are null
         */
        public EmailMessage build() {
            return new EmailMessage(this);
        }
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
                "to='" + to + '\'' +
                ", from='" + from + '\'' +
                ", subject='" + subject + '\'' +
                ", hasHtmlBody=" + (htmlBody != null) +
                ", hasTextBody=" + (textBody != null) +
                ", templateDataKeys=" + templateData.keySet() +
                '}';
    }
}
