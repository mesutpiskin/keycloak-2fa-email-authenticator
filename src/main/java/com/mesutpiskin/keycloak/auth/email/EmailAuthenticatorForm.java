package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.credential.CredentialProvider;

import org.jboss.logging.Logger;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailAuthenticatorForm extends AbstractUsernameFormAuthenticator
        implements CredentialValidator<EmailAuthenticatorCredentialProvider> {

    protected static final Logger logger = Logger.getLogger(EmailAuthenticatorForm.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        challenge(context, null);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        generateAndSendEmailCode(context);

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }
        Response response = form.createForm("email-code-form.ftl");
        context.challenge(response);
        return response;
    }

    private void generateAndSendEmailCode(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        if (session.getAuthNote(EmailConstants.CODE) != null) {
            // skip sending email code
            return;
        }

        Map<String, String> configValues = config != null && config.getConfig() != null
                ? config.getConfig()
                : Map.of();

        int length = resolvePositiveInt(configValues, EmailConstants.CODE_LENGTH, EmailConstants.DEFAULT_LENGTH);
        int ttl = resolvePositiveInt(configValues, EmailConstants.CODE_TTL, EmailConstants.DEFAULT_TTL);
        int resendCooldown = resolvePositiveInt(configValues, EmailConstants.RESEND_COOLDOWN,
                EmailConstants.DEFAULT_RESEND_COOLDOWN);

        String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
        sendEmailWithCode(context.getSession(), context.getRealm(), context.getUser(), code, ttl);
        session.setAuthNote(EmailConstants.CODE, code);
        long now = System.currentTimeMillis();
        session.setAuthNote(EmailConstants.CODE_TTL, Long.toString(now + (ttl * 1000L)));
        session.setAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER, Long.toString(now + (resendCooldown * 1000L)));
    }

    private int resolvePositiveInt(Map<String, String> configValues, String key, int defaultValue) {
        String raw = configValues.get(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed <= 0) {
                logger.warnf("Configuration value for %s was non-positive ('%s'); falling back to default %d", key, raw,
                        defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            logger.warnf("Configuration value for %s was invalid ('%s'); falling back to default %d", key, raw,
                    defaultValue);
            return defaultValue;
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        UserModel userModel = context.getUser();
        if (!enabledUser(context, userModel)) {
            // error in context is set in enabledUser/isDisabledByBruteForce
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (handleFormShortcuts(context, formData)) {
            return;
        }

        CodeContext codeContext = buildCodeContext(context.getAuthenticationSession(), formData);
        if (!validateCodeContext(context, userModel, codeContext)) {
            return;
        }

        if (codeContext.expiresAt() < System.currentTimeMillis()) {
            handleExpiredCode(context, userModel);
            return;
        }

        if (codeContext.submittedCode().equals(codeContext.storedCode())) {
            handleSuccessfulValidation(context);
        } else {
            handleInvalidCode(context, userModel);
        }
    }

    private boolean handleFormShortcuts(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        if (formData.containsKey("resend")) {
            AuthenticationSessionModel session = context.getAuthenticationSession();
            String resendAvailableRaw = session.getAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
            Long resendAvailableAt = null;
            if (resendAvailableRaw != null) {
                try {
                    resendAvailableAt = Long.parseLong(resendAvailableRaw);
                } catch (NumberFormatException ex) {
                    logger.warnf("Invalid resend availability timestamp '%s' for email authenticator; allowing resend",
                            resendAvailableRaw);
                }
            }

            long now = System.currentTimeMillis();
            if (resendAvailableAt != null && now < resendAvailableAt) {
                long millisRemaining = resendAvailableAt - now;
                long secondsRemaining = Math.max(1L, (millisRemaining + 999L) / 1000L);
                LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
                form.setError("email-authenticator-resend-cooldown", secondsRemaining);
                Response response = form.createForm("email-code-form.ftl");
                context.challenge(response);
                return true;
            }

            resetEmailCode(context);
            challenge(context, null);
            return true;
        }

        if (formData.containsKey("cancel")) {
            resetEmailCode(context);
            context.resetFlow();
            return true;
        }

        return false;
    }

    private CodeContext buildCodeContext(AuthenticationSessionModel session, MultivaluedMap<String, String> formData) {
        String storedCode = session.getAuthNote(EmailConstants.CODE);
        String ttlNote = session.getAuthNote(EmailConstants.CODE_TTL);
        Long expiresAt = null;
        if (ttlNote != null) {
            try {
                expiresAt = Long.parseLong(ttlNote);
            } catch (NumberFormatException ex) {
                logger.warnf("Invalid TTL value '%s' found for email authenticator; treating as expired", ttlNote);
            }
        }

        String submittedRaw = formData.getFirst(EmailConstants.CODE);
        String submittedCode = submittedRaw == null ? null : submittedRaw.strip();

        return new CodeContext(storedCode, expiresAt, submittedCode);
    }

    private boolean validateCodeContext(AuthenticationFlowContext context, UserModel user, CodeContext codeContext) {
        if (codeContext.storedCode() == null || codeContext.expiresAt() == null) {
            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_ACCESS_CODE, EmailConstants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }

        if (codeContext.submittedCode() == null || codeContext.submittedCode().isEmpty()) {
            Response challengeResponse = challenge(context, Messages.MISSING_TOTP, EmailConstants.CODE);
            context.challenge(challengeResponse);
            return false;
        }

        return true;
    }

    private void handleExpiredCode(AuthenticationFlowContext context, UserModel user) {
        context.getEvent().user(user).error(Errors.EXPIRED_CODE);
        Response challengeResponse = challenge(context, Messages.EXPIRED_ACTION_TOKEN_SESSION_EXISTS,
                EmailConstants.CODE);
        context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challengeResponse);
    }

    private void handleSuccessfulValidation(AuthenticationFlowContext context) {
        resetEmailCode(context);
        context.success();
    }

    private void handleInvalidCode(AuthenticationFlowContext context, UserModel user) {
        AuthenticationExecutionModel execution = context.getExecution();
        if (execution.isRequired()) {
            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_ACCESS_CODE, EmailConstants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        } else if (execution.isConditional() || execution.isAlternative()) {
            context.attempted();
        }
    }

    @Override
    protected String disabledByBruteForceError() {
        return Messages.INVALID_ACCESS_CODE;
    }

    private void resetEmailCode(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        session.removeAuthNote(EmailConstants.CODE);
        session.removeAuthNote(EmailConstants.CODE_TTL);
        session.removeAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
    }

    private record CodeContext(String storedCode, Long expiresAt, String submittedCode) {
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return getCredentialProvider(session).isConfiguredFor(realm, user, getType(session));
    }

    @Override
    public EmailAuthenticatorCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (EmailAuthenticatorCredentialProvider) session.getProvider(CredentialProvider.class,
                EmailAuthenticatorCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(EmailAuthenticatorRequiredAction.PROVIDER_ID);
    }

    @Override
    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {
        return Collections.singletonList((EmailAuthenticatorRequiredActionFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RequiredActionProvider.class, EmailAuthenticatorRequiredAction.PROVIDER_ID));
    }

    @Override
    public void close() {
        // NOOP
    }

    private void sendEmailWithCode(KeycloakSession session, RealmModel realm, UserModel user, String code, int ttl) {
        if (user.getEmail() == null) {
            logger.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);
        mailBodyAttributes.put("ttl", ttl);

        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = List.of(realmName);
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the welcome-email.ftl (html and text) template to your theme.
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            logger.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }
}
