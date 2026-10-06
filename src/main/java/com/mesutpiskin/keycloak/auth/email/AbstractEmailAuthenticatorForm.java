package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.email.EmailException;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.SecretGenerator;

import org.jboss.logging.Logger;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared implementation of the email one-time-password (OTP) login step.
 * <p>
 * This class owns everything about the OTP challenge itself: generating and
 * hashing the code, sending it through the configured email provider, rendering
 * {@code email-code-form.ftl}, resend cooldown, expiry, attempt limiting and
 * brute-force integration. It deliberately says nothing about <em>who</em> is
 * eligible for the step; that is the job of {@link #configuredFor}, which each
 * concrete authenticator implements.
 * </p>
 * <p>
 * The split exists because Keycloak classifies authenticators by type, not by
 * configuration: an authenticator implementing
 * {@link org.keycloak.authentication.CredentialValidator CredentialValidator} is
 * treated as credential-based and is only offered under "Try Another Way" to
 * users who have a stored credential of its type. A single class cannot be both,
 * so the two eligibility models live in two thin subclasses:
 * </p>
 * <ul>
 * <li>{@link EmailAuthenticatorForm} ({@code email-authenticator}) — credential
 * based; eligible only after the user has enrolled.</li>
 * <li>{@link NoEnrollmentEmailAuthenticatorForm}
 * ({@code email-authenticator-no-enrollment}) — not credential based; eligible
 * for any user with an email address.</li>
 * </ul>
 * <p>
 * New OTP behaviour belongs here so that every variant picks it up. New
 * eligibility rules belong in a subclass.
 * </p>
 *
 * @author Mesut Pişkin
 * @since 26.7.0
 */
public abstract class AbstractEmailAuthenticatorForm extends AbstractUsernameFormAuthenticator {

    protected static final Logger logger = Logger.getLogger(AbstractEmailAuthenticatorForm.class);
    private static final String CODE_ATTEMPTS = "emailCodeAttempts";

    /**
     * Initiates the authentication process by presenting the email OTP challenge to
     * the user.
     * <p>
     * This method is called by Keycloak when the user reaches this authenticator in
     * the flow.
     * It generates and sends an email code, then displays the form for code entry.
     * </p>
     *
     * @param context the authentication flow context containing user, session, and
     *                realm information
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(challenge(context, null));
    }

    /**
     * Creates the authentication challenge response with the email code entry form.
     * <p>
     * Generates and sends the email code if not already sent, prepares the form
     * with
     * any error messages, and returns the rendered form response.
     * </p>
     *
     * @param context the authentication flow context
     * @param error   optional error message key to display
     * @param field   optional field name associated with the error
     * @return the HTTP response containing the rendered form
     */
    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        generateAndSendEmailCode(context);
        LoginFormsProvider form = prepareForm(context, null);
        applyFormMessage(form, error, field);
        return form.createForm("email-code-form.ftl");
    }

    /**
     * Generates a random email code and sends it to the user's registered email
     * address.
     * <p>
     * If a code has already been generated for this session, this method returns
     * early
     * to prevent duplicate emails. The code is stored in the authentication session
     * along
     * with its expiration time and resend cooldown period.
     * </p>
     * <p>
     * In simulation mode, the code is logged instead of being emailed, useful for
     * development.
     * </p>
     *
     * @param context the authentication flow context
     */
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
        if (Boolean.parseBoolean(configValues.get(EmailConstants.SIMULATION_MODE))) {
            logger.infof("***** SIMULATION MODE ***** Email code send to %s for user %s is: %s",
                    context.getUser().getEmail(), context.getUser().getUsername(), code);
        } else {
            sendEmailWithCode(context, code, ttl);
        }

        session.setAuthNote(EmailConstants.CODE, OtpHashUtils.hash(code));
        long now = System.currentTimeMillis();
        session.setAuthNote(EmailConstants.CODE_TTL, Long.toString(now + (ttl * 1000L)));
        session.setAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER, Long.toString(now + (resendCooldown * 1000L)));
    }

    /**
     * Resolves a positive integer configuration value with validation and fallback.
     * <p>
     * Parses the configuration value for the given key. If the value is missing,
     * blank,
     * not a valid integer, or non-positive, returns the default value and logs a
     * warning.
     * </p>
     *
     * @param configValues the configuration map
     * @param key          the configuration key to resolve
     * @param defaultValue the fallback value if parsing fails or value is invalid
     * @return the parsed positive integer or the default value
     */
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

    /**
     * Processes the form submission when the user enters the email code.
     * <p>
     * Validates the submitted code against the stored code, checking for expiration
     * and correctness. Handles special form actions like "resend" and "cancel".
     * On successful validation, marks the authentication as successful.
     * </p>
     *
     * @param context the authentication flow context
     */
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

        if (isValidCodeContext(context, userModel, formData)) {
            resetEmailCode(context);
            // Marked as an "otp" success, as Keycloak's own OTP form does. Wrong codes
            // are reported under "otp" (see isValidCodeContext), and from 26.6 on that
            // also feeds the realm's secondary-factor counter, which only a login that
            // includes an otp credential clears. A plain success() would leave that
            // counter growing with every typo until "Max secondary auth failures"
            // locks the user permanently.
            context.success(BruteForceFailureReporter.CATEGORY);
        }
    }

    private boolean handleFormShortcuts(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        if (formData.containsKey("resend")) {
            AuthenticationSessionModel session = context.getAuthenticationSession();
            Long remainingSeconds = getRemainingSeconds(session);
            if (remainingSeconds != null && remainingSeconds > 0L) {
                LoginFormsProvider form = prepareForm(context, remainingSeconds);
                applyFormMessage(form, "email-authenticator-resend-cooldown", null, remainingSeconds);
                context.challenge(form.createForm("email-code-form.ftl"));
                return true;
            }

            resetEmailCode(context);
            context.challenge(challenge(context, null));
            return true;
        }

        if (formData.containsKey("cancel")) {
            resetEmailCode(context);
            context.resetFlow();
            return true;
        }

        return false;
    }

    private record CodeContext(String storedCode, Long expiresAt, String submittedCode) {
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

    private boolean isValidCodeContext(AuthenticationFlowContext context, UserModel user,
            MultivaluedMap<String, String> formData) {
        CodeContext codeContext = buildCodeContext(context.getAuthenticationSession(), formData);
        if (codeContext.storedCode() == null || codeContext.expiresAt() == null) {
            context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_ACCESS_CODE, EmailConstants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }

        if (codeContext.submittedCode() == null || codeContext.submittedCode().isEmpty()) {
            context.challenge(challenge(context, Messages.MISSING_TOTP, EmailConstants.CODE));
            return false;
        }

        if (codeContext.expiresAt() < System.currentTimeMillis()) {
            context.getEvent().user(user).error(Errors.EXPIRED_CODE);
            Response challengeResponse = challenge(context, Messages.EXPIRED_ACTION_TOKEN_SESSION_EXISTS,
                    EmailConstants.CODE);
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challengeResponse);
            return false;
        }

        if (OtpHashUtils.matches(codeContext.submittedCode(), codeContext.storedCode())) {
            return true;
        }

        context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);

        // The per-code attempt limit applies whether or not the realm has brute
        // force protection enabled. It used to be skipped when protection was on,
        // on the assumption that Keycloak would count the failure instead.
        AuthenticationSessionModel session = context.getAuthenticationSession();
        int attempts = incrementAttempts(session);

        if (context.getRealm().isBruteForceProtected()) {
            // From Keycloak 26.6 on, the failureChallenge below is not counted: the
            // brute force protector only accepts the password, otp and recovery-code
            // categories and drops this authenticator's own reference category. The
            // wrong code is therefore reported explicitly, under "otp", so the realm
            // lockout applies.
            //
            // On 26.5 and earlier there is no category filter and the
            // failureChallenge is already counted, so the reporter does nothing
            // there; a second report would count each wrong code twice. The same
            // holds if a future Keycloak starts counting our reference category:
            // this call must then go.
            BruteForceFailureReporter.report(context, user);
        }

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        Map<String, String> configValues = config != null && config.getConfig() != null
                ? config.getConfig()
                : Map.of();
        int maxAttempts = resolvePositiveInt(configValues, EmailConstants.MAX_ATTEMPTS,
                EmailConstants.DEFAULT_MAX_ATTEMPTS);

        if (attempts >= maxAttempts) {
            resetEmailCode(context);
            LoginFormsProvider form = prepareForm(context, null);
            form.setAttribute("maxAttemptsReached", true);
            applyFormMessage(form, "email-authenticator-too-many-attempts", EmailConstants.CODE);
            Response challengeResponse = form.createForm("email-code-form.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        } else {
            Response challengeResponse = challenge(context, Messages.INVALID_ACCESS_CODE, EmailConstants.CODE);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }
        return false;
    }

    private LoginFormsProvider prepareForm(AuthenticationFlowContext context, Long remainingSeconds) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        Long secondsToExpose = remainingSeconds != null ? remainingSeconds : getRemainingSeconds(session);
        if (secondsToExpose != null && secondsToExpose > 0L) {
            form.setAttribute("resendAvailableInSeconds", secondsToExpose);
        }

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        Map<String, String> configValues = config != null && config.getConfig() != null
                ? config.getConfig()
                : Map.of();
        int codeLength = resolvePositiveInt(configValues, EmailConstants.CODE_LENGTH, EmailConstants.DEFAULT_LENGTH);
        form.setAttribute("codeLength", codeLength);

        EmailMasking.applyToForm(form, context.getUser(), configValues);

        return form;
    }

    private Long getRemainingSeconds(AuthenticationSessionModel session) {
        String rawResendAfter = session.getAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
        if (rawResendAfter == null) {
            return null;
        }
        Long resendAt = null;
        try {
            resendAt = Long.parseLong(rawResendAfter);
        } catch (NumberFormatException ex) {
            logger.warnf("Invalid resend availability timestamp '%s' for email authenticator; allowing resend",
                    rawResendAfter);
            session.removeAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER);
            return null;
        }
        long remainingMillis = resendAt - System.currentTimeMillis();
        return Math.max(0L, (remainingMillis + EmailConstants.MILLIS_ROUNDING_OFFSET) / 1000L);
    }

    private void applyFormMessage(LoginFormsProvider form, String messageKey, String field, Object... messageParams) {
        if (messageKey == null) {
            return;
        }
        if (field != null) {
            form.addError(new FormMessage(field, messageKey, messageParams));
        } else {
            form.setError(messageKey, messageParams);
        }
    }

    protected String disabledByBruteForceError() {
        return Messages.INVALID_ACCESS_CODE;
    }

    private void resetEmailCode(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
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

    @Override
    public boolean requiresUser() {
        return true;
    }

    /**
     * Reports whether this authenticator can run for {@code user}. Each concrete
     * authenticator defines its own eligibility model; see the subclasses for the
     * exact rules.
     * <p>
     * Implementations must return {@code false} for a user without a usable email
     * address (see {@link #hasEmail(UserModel)}): there is nowhere to send the
     * code, and Keycloak relies on this method to skip or reject the execution
     * before {@link #authenticate(AuthenticationFlowContext)} is reached.
     * </p>
     */
    @Override
    public abstract boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user);

    /**
     * Intentionally a no-op: the login flow never assigns the enrolment required
     * action automatically. Enrolment stays voluntary (account console) or
     * admin-assigned via {@link EmailAuthenticatorRequiredAction#PROVIDER_ID}.
     */
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // intentional no-op
    }

    @Override
    public void close() {
        // NOOP
    }

    /**
     * Returns {@code true} when {@code user} has a non-blank email address, i.e.
     * when there is somewhere to send a code. Shared building block for the
     * {@link #configuredFor} implementations.
     *
     * @param user the user to check; may be {@code null}
     * @return whether the user has a usable email address
     */
    protected static boolean hasEmail(UserModel user) {
        if (user == null) {
            return false;
        }
        String email = user.getEmail();
        return email != null && !email.isBlank();
    }

    private void sendEmailWithCode(AuthenticationFlowContext context, String code, int ttl) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        if (user.getEmail() == null) {
            logger.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(),
                    user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        // Build email message with template data
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("username", user.getUsername());
        templateData.put("code", code);
        templateData.put("ttl", ttl);

        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        String subject = realmName + " access code";

        com.mesutpiskin.keycloak.auth.email.model.EmailMessage message = com.mesutpiskin.keycloak.auth.email.model.EmailMessage
                .builder()
                .to(user.getEmail())
                .subject(subject)
                .templateData(templateData)
                .build();

        // Determine email provider from config
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        Map<String, String> configMap = config != null && config.getConfig() != null
                ? config.getConfig()
                : Map.of();

        String providerTypeStr = configMap.getOrDefault(
                EmailConstants.EMAIL_PROVIDER_TYPE,
                EmailConstants.DEFAULT_EMAIL_PROVIDER);
        com.mesutpiskin.keycloak.auth.email.model.EmailProviderType providerType = com.mesutpiskin.keycloak.auth.email.model.EmailProviderType
                .fromString(providerTypeStr);

        try {
            // Create email sender based on configuration
            com.mesutpiskin.keycloak.auth.email.service.EmailSender emailSender = com.mesutpiskin.keycloak.auth.email.service.EmailSenderFactory
                    .createEmailSender(
                            providerType,
                            configMap,
                            session,
                            realm,
                            user);

            // Send email
            emailSender.sendEmail(message);
            logger.infof("Email sent successfully via %s to %s",
                    emailSender.getProviderName(), user.getEmail());

        } catch (EmailException e) {
            // Fallback to Keycloak SMTP if enabled
            boolean fallbackEnabled = com.mesutpiskin.keycloak.auth.email.service.EmailSenderFactory
                    .isFallbackEnabled(configMap);

            if (fallbackEnabled
                    && providerType != com.mesutpiskin.keycloak.auth.email.model.EmailProviderType.KEYCLOAK) {
                logger.warnf(e, "Primary email provider (%s) failed, falling back to Keycloak SMTP",
                        providerType.getDisplayName());
                try {
                    com.mesutpiskin.keycloak.auth.email.service.EmailSender fallbackSender = new com.mesutpiskin.keycloak.auth.email.service.impl.KeycloakEmailSender(
                            session, realm, user);
                    fallbackSender.sendEmail(message);
                    logger.infof("Email sent successfully via fallback Keycloak SMTP to %s", user.getEmail());
                } catch (EmailException fallbackEx) {
                    logger.errorf(fallbackEx, "Fallback email provider also failed. realm=%s user=%s",
                            realm.getId(), user.getUsername());
                }
            } else {
                logger.errorf(e, "Failed to send access code email. realm=%s user=%s",
                        realm.getId(), user.getUsername());
            }
        }
    }
}
