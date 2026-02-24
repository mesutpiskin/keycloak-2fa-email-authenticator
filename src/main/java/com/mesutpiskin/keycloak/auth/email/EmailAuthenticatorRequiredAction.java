package com.mesutpiskin.keycloak.auth.email;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.mesutpiskin.keycloak.auth.email.model.EmailMessage;
import com.mesutpiskin.keycloak.auth.email.model.EmailProviderType;
import com.mesutpiskin.keycloak.auth.email.service.EmailSender;
import com.mesutpiskin.keycloak.auth.email.service.EmailSenderFactory;
import com.mesutpiskin.keycloak.auth.email.service.impl.KeycloakEmailSender;

import org.keycloak.models.AuthenticatorConfigModel;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class EmailAuthenticatorRequiredAction implements RequiredActionProvider, CredentialRegistrator {

    public static final String PROVIDER_ID = "email-authenticator-setup";
    private static final String SETUP_TEMPLATE = "email-authenticator-setup-form.ftl";
    private static final String VERIFY_TEMPLATE = "email-authenticator-setup-verify-form.ftl";
    private static final Logger logger = Logger.getLogger(EmailAuthenticatorRequiredAction.class);
    private static final String CODE_ATTEMPTS = "emailCodeAttempts";

    private enum CodeValidationResult {
        VALID, EXPIRED, INVALID, MISSING
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // Not needed for self-service setup
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        if (userMissingEmail(context.getUser())) {
            context.challenge(context.form()
                    .setError("email-authenticator-setup-missing-email")
                    .createForm(SETUP_TEMPLATE));
            return;
        }

        context.challenge(context.form().createForm(SETUP_TEMPLATE));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        UserModel user = context.getUser();
        AuthenticationSessionModel session = context.getAuthenticationSession();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        // Handle cancel
        if (formData.containsKey("cancel") || formData.containsKey("cancel-aia")) {
            if (session.getAuthNote(EmailConstants.CODE) != null) {
                // OTP was sent — cancel goes back to setup form
                resetSetupCode(session);
                requiredActionChallenge(context);
                return;
            }
            // No OTP sent yet — handle cancel-aia for app-initiated actions
            if (formData.containsKey("cancel-aia") && isAppInitiatedAction(context)) {
                if (!cancelLogin(context)) {
                    context.failure();
                }
            } else {
                context.challenge(context.form()
                        .setError("email-authenticator-setup-cancelled")
                        .createForm(SETUP_TEMPLATE));
            }
            return;
        }

        // Handle resend
        if (formData.containsKey("resend")) {
            Long remainingSeconds = getRemainingCooldownSeconds(session);
            if (remainingSeconds != null && remainingSeconds > 0L) {
                challengeVerifyForm(context, "email-authenticator-resend-cooldown", remainingSeconds);
                return;
            }
            resetSetupCode(session);
            generateAndSendSetupCode(context);
            return;
        }

        // Phase 1: OTP not yet sent — user clicked "Enable Email Authenticator"
        if (session.getAuthNote(EmailConstants.CODE) == null) {
            if (userMissingEmail(user)) {
                context.challenge(context.form()
                        .setError("email-authenticator-setup-missing-email")
                        .createForm(SETUP_TEMPLATE));
                return;
            }

            if (hasExistingCredential(user)) {
                user.removeRequiredAction(PROVIDER_ID);
                context.success();
                return;
            }

            generateAndSendSetupCode(context);
            return;
        }

        // Phase 2: OTP was sent — user is submitting the code
        String submittedRaw = formData.getFirst(EmailConstants.CODE);
        String submittedCode = submittedRaw == null ? null : submittedRaw.strip();

        CodeValidationResult result = isValidSetupCode(session, submittedCode);
        switch (result) {
            case VALID:
                resetSetupCode(session);
                EmailAuthenticatorCredentialModel credential = EmailAuthenticatorCredentialModel.create();
                credential.setUserLabel(user.getEmail());
                try {
                    user.credentialManager().createStoredCredential(credential);
                    user.removeRequiredAction(PROVIDER_ID);
                    context.success();
                } catch (RuntimeException ex) {
                    logger.errorf(ex, "Failed to persist email authenticator credential for user %s", user.getId());
                    context.challenge(context.form()
                            .setError("email-authenticator-setup-error")
                            .createForm(SETUP_TEMPLATE));
                }
                break;
            case EXPIRED:
                resetSetupCode(session);
                challengeVerifyForm(context, Messages.EXPIRED_CODE);
                break;
            case MISSING:
                challengeVerifyForm(context, Messages.MISSING_CODE);
                break;
            case INVALID:
                Map<String, String> configMap = findAuthenticatorConfig(context);
                int maxAttempts = resolvePositiveInt(configMap, EmailConstants.MAX_ATTEMPTS,
                        EmailConstants.DEFAULT_MAX_ATTEMPTS);
                int attempts = incrementAttempts(session);
                if (attempts >= maxAttempts) {
                    resetSetupCode(session);
                    var form = context.form();
                    form.setAttribute("maxAttemptsReached", true);
                    form.setError(Messages.TOO_MANY_ATTEMPTS);
                    context.challenge(form.createForm(VERIFY_TEMPLATE));
                } else {
                    challengeVerifyForm(context, Messages.INVALID_CODE);
                }
                break;
        }
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSessionModel) {
        return EmailAuthenticatorCredentialModel.TYPE_ID;
    }

    private void generateAndSendSetupCode(RequiredActionContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        UserModel user = context.getUser();
        KeycloakSession keycloakSession = context.getSession();
        RealmModel realm = context.getRealm();

        Map<String, String> configMap = findAuthenticatorConfig(context);

        int length = resolvePositiveInt(configMap, EmailConstants.CODE_LENGTH, EmailConstants.DEFAULT_LENGTH);
        int ttl = resolvePositiveInt(configMap, EmailConstants.CODE_TTL, EmailConstants.DEFAULT_TTL);
        int resendCooldown = resolvePositiveInt(configMap, EmailConstants.RESEND_COOLDOWN,
                EmailConstants.DEFAULT_RESEND_COOLDOWN);

        String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);

        if (Boolean.parseBoolean(configMap.get(EmailConstants.SIMULATION_MODE))) {
            logger.infof("***** SIMULATION MODE ***** Setup verification code for user %s is: %s",
                    user.getUsername(), code);
        } else {
            try {
                Map<String, Object> templateData = new HashMap<>();
                templateData.put("username", user.getUsername());
                templateData.put("code", code);
                templateData.put("ttl", ttl);

                String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();

                EmailMessage message = EmailMessage.builder()
                        .to(user.getEmail())
                        .subject(realmName + " access code")
                        .templateData(templateData)
                        .build();

                sendEmail(message, configMap, keycloakSession, realm, user);
            } catch (EmailException e) {
                logger.errorf(e, "Failed to send setup verification email for user %s", user.getId());
                context.challenge(context.form()
                        .setError("email-authenticator-setup-send-error")
                        .createForm(SETUP_TEMPLATE));
                return;
            }
        }

        long now = System.currentTimeMillis();
        session.setAuthNote(EmailConstants.CODE, code);
        session.setAuthNote(EmailConstants.CODE_TTL, Long.toString(now + (ttl * 1000L)));
        session.setAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER, Long.toString(now + (resendCooldown * 1000L)));

        challengeVerifyForm(context, null);
    }

    private void sendEmail(EmailMessage message, Map<String, String> configMap,
            KeycloakSession session, RealmModel realm, UserModel user) throws EmailException {
        String providerTypeStr = configMap.getOrDefault(
                EmailConstants.EMAIL_PROVIDER_TYPE,
                EmailConstants.DEFAULT_EMAIL_PROVIDER);
        EmailProviderType providerType = EmailProviderType.fromString(providerTypeStr);

        try {
            EmailSender emailSender = EmailSenderFactory.createEmailSender(
                    providerType, configMap, session, realm, user);
            emailSender.sendEmail(message);
            logger.infof("Setup verification email sent via %s to %s",
                    emailSender.getProviderName(), user.getEmail());
        } catch (EmailException e) {
            boolean fallbackEnabled = EmailSenderFactory.isFallbackEnabled(configMap);
            if (fallbackEnabled && providerType != EmailProviderType.KEYCLOAK) {
                logger.warnf(e, "Primary email provider (%s) failed for setup, falling back to Keycloak SMTP",
                        providerType.getDisplayName());
                EmailSender fallbackSender = new KeycloakEmailSender(session, realm, user);
                fallbackSender.sendEmail(message);
                logger.infof("Setup verification email sent via fallback Keycloak SMTP to %s", user.getEmail());
            } else {
                throw e;
            }
        }
    }

    private Map<String, String> findAuthenticatorConfig(RequiredActionContext context) {
        RealmModel realm = context.getRealm();

        return realm.getAuthenticationFlowsStream()
                .flatMap(flow -> realm.getAuthenticationExecutionsStream(flow.getId()))
                .filter(exec -> EmailAuthenticatorFormFactory.PROVIDER_ID.equals(exec.getAuthenticator()))
                .map(exec -> {
                    String configId = exec.getAuthenticatorConfig();
                    if (configId != null) {
                        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(configId);
                        if (config != null && config.getConfig() != null) {
                            return config.getConfig();
                        }
                    }
                    return Map.<String, String>of();
                })
                .findFirst()
                .orElse(Map.of());
    }

    private int resolvePositiveInt(Map<String, String> configValues, String key, int defaultValue) {
        String raw = configValues.get(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed <= 0) {
                logger.warnf("Configuration value for %s was non-positive ('%s'); falling back to default %d",
                        key, raw, defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            logger.warnf("Configuration value for %s was invalid ('%s'); falling back to default %d",
                    key, raw, defaultValue);
            return defaultValue;
        }
    }

    private void challengeVerifyForm(RequiredActionContext context, String error, Object... errorParams) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        var form = context.form();

        Long remaining = getRemainingCooldownSeconds(session);
        if (remaining != null && remaining > 0L) {
            form.setAttribute("resendAvailableInSeconds", remaining);
        }

        if (error != null) {
            form.setError(error, errorParams);
        }

        Response response = form.createForm(VERIFY_TEMPLATE);
        context.challenge(response);
    }

    private void resetSetupCode(AuthenticationSessionModel session) {
        session.removeAuthNote(EmailConstants.CODE);
        session.removeAuthNote(EmailConstants.CODE_TTL);
        session.removeAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
        session.removeAuthNote(CODE_ATTEMPTS);
    }

    private int incrementAttempts(AuthenticationSessionModel session) {
        String raw = session.getAuthNote(CODE_ATTEMPTS);
        int attempts = 1;
        if (raw != null) {
            try {
                attempts = Integer.parseInt(raw) + 1;
            } catch (NumberFormatException ignored) {
                // corrupt value, start over
            }
        }
        session.setAuthNote(CODE_ATTEMPTS, Integer.toString(attempts));
        return attempts;
    }

    private CodeValidationResult isValidSetupCode(AuthenticationSessionModel session, String submittedCode) {
        String storedCode = session.getAuthNote(EmailConstants.CODE);
        String ttlNote = session.getAuthNote(EmailConstants.CODE_TTL);

        if (storedCode == null || ttlNote == null) {
            return CodeValidationResult.MISSING;
        }

        if (submittedCode == null || submittedCode.isEmpty()) {
            return CodeValidationResult.MISSING;
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(ttlNote);
        } catch (NumberFormatException ex) {
            logger.warnf("Invalid TTL value '%s' in setup verification; treating as expired", ttlNote);
            return CodeValidationResult.EXPIRED;
        }

        if (expiresAt < System.currentTimeMillis()) {
            return CodeValidationResult.EXPIRED;
        }

        if (MessageDigest.isEqual(submittedCode.getBytes(), storedCode.getBytes())) {
            return CodeValidationResult.VALID;
        }

        return CodeValidationResult.INVALID;
    }

    private Long getRemainingCooldownSeconds(AuthenticationSessionModel session) {
        String rawResendAfter = session.getAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
        if (rawResendAfter == null) {
            return null;
        }
        try {
            long resendAt = Long.parseLong(rawResendAfter);
            long remainingMillis = resendAt - System.currentTimeMillis();
            return Math.max(0L, (remainingMillis + EmailConstants.MILLIS_ROUNDING_OFFSET) / 1000L);
        } catch (NumberFormatException ex) {
            logger.warnf("Invalid resend availability timestamp '%s'; allowing resend", rawResendAfter);
            session.removeAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
            return null;
        }
    }

    private boolean hasExistingCredential(UserModel user) {
        return user.credentialManager()
                .getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID)
                .findAny()
                .isPresent();
    }

    private boolean userMissingEmail(UserModel user) {
        return user.getEmail() == null || user.getEmail().isBlank();
    }

    private boolean isAppInitiatedAction(RequiredActionContext context) {
        try {
            Method method = context.getClass().getMethod("isAppInitiatedAction");
            Object result = method.invoke(context);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ex) {
            logger.debugf("RequiredActionContext.isAppInitiatedAction unavailable: %s", ex.getMessage());
            return false;
        }
    }

    private boolean cancelLogin(RequiredActionContext context) {
        try {
            Method method = context.getClass().getMethod("cancelLogin");
            method.invoke(context);
            return true;
        } catch (ReflectiveOperationException ex) {
            logger.debugf("RequiredActionContext.cancelLogin unavailable: %s", ex.getMessage());
            return false;
        }
    }

    private static final class Messages {
        static final String EXPIRED_CODE = "email-authenticator-setup-code-expired";
        static final String INVALID_CODE = "email-authenticator-setup-code-invalid";
        static final String MISSING_CODE = "email-authenticator-setup-code-missing";
        static final String TOO_MANY_ATTEMPTS = "email-authenticator-setup-too-many-attempts";
    }
}
