package com.mesutpiskin.keycloak.auth.email.service.impl;

import com.mesutpiskin.keycloak.auth.email.model.EmailMessage;
import com.mesutpiskin.keycloak.auth.email.service.EmailSender;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;

/**
 * Default email sender implementation using Keycloak's built-in SMTP
 * functionality.
 * <p>
 * This implementation wraps Keycloak's {@link EmailTemplateProvider} to
 * maintain
 * backward compatibility with existing SMTP configurations. It uses the email
 * settings configured in Keycloak's Realm Settings.
 * </p>
 * 
 * <p>
 * This is the default provider and requires no additional configuration beyond
 * standard Keycloak email setup.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.1.0
 */
public class KeycloakEmailSender implements EmailSender {

    private static final Logger logger = Logger.getLogger(KeycloakEmailSender.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel user;

    /**
     * Constructs a new KeycloakEmailSender.
     *
     * @param session the Keycloak session
     * @param realm   the realm model
     * @param user    the user to send email to
     */
    public KeycloakEmailSender(KeycloakSession session, RealmModel realm, UserModel user) {
        this.session = session;
        this.realm = realm;
        this.user = user;
    }

    @Override
    public void sendEmail(EmailMessage message) throws EmailException {
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);

            // Extract realm name for subject
            String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
            List<Object> subjectParams = List.of(realmName);

            // Send email using Keycloak's template system
            // The template data should contain: username, code, ttl
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", message.getTemplateData());

            logger.debugf("Email sent successfully via Keycloak SMTP to %s", message.getTo());

        } catch (EmailException e) {
            logger.errorf(e, "Failed to send email via Keycloak SMTP to %s", message.getTo());
            throw e;
        }
    }

    @Override
    public String getProviderName() {
        return "Keycloak SMTP";
    }

    @Override
    public boolean isAvailable() {
        // Check if realm has SMTP configured
        if (realm.getSmtpConfig() == null || realm.getSmtpConfig().isEmpty()) {
            logger.warnf("Keycloak SMTP not configured for realm %s", realm.getName());
            return false;
        }
        return true;
    }
}
