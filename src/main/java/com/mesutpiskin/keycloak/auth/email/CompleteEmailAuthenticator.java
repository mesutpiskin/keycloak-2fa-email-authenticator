package com.mesutpiskin.keycloak.auth.email;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JBossLog
public class CompleteEmailAuthenticator implements Authenticator {

  private static final String EMAIL_STEP = "EMAIL_STEP";
  private static final String OTP_STEP = "OTP_STEP";
  private static final String CURRENT_STEP = "CURRENT_STEP";
  private static final String USER_EMAIL = "USER_EMAIL";

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    AuthenticationSessionModel session = context.getAuthenticationSession();
    String currentStep = session.getAuthNote(CURRENT_STEP);

    if (currentStep == null) {
      // Start with email collection
      session.setAuthNote(CURRENT_STEP, EMAIL_STEP);
      showEmailForm(context, null);
    } else if (EMAIL_STEP.equals(currentStep)) {
      showEmailForm(context, null);
    } else if (OTP_STEP.equals(currentStep)) {
      showOtpForm(context, null);
    }
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    AuthenticationSessionModel session = context.getAuthenticationSession();
    String currentStep = session.getAuthNote(CURRENT_STEP);

    if (EMAIL_STEP.equals(currentStep)) {
      handleEmailStep(context);
    } else if (OTP_STEP.equals(currentStep)) {
      handleOtpStep(context);
    }
  }

  private void handleEmailStep(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    String email = formData.getFirst("email");

    if (email == null || email.trim().isEmpty()) {
      showEmailForm(context, "Email is required");
      return;
    }

    // Validate email format
    if (!isValidEmail(email)) {
      showEmailForm(context, "Invalid email format");
      return;
    }

    AuthenticationSessionModel session = context.getAuthenticationSession();
    session.setAuthNote(USER_EMAIL, email);

    // Find or create user
    UserModel user = findOrCreateUser(context, email);
    if (user == null) {
      showEmailForm(context, "Failed to process user");
      return;
    }

    context.setUser(user);

    // Move to OTP step
    session.setAuthNote(CURRENT_STEP, OTP_STEP);
    generateAndSendEmailCode(context);
    showOtpForm(context, null);
  }

  private void handleOtpStep(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

    if (formData.containsKey("resend")) {
      generateAndSendEmailCode(context);
      showOtpForm(context, "Code resent to your email");
      return;
    }

    if (formData.containsKey("back")) {
      // Go back to email step
      AuthenticationSessionModel session = context.getAuthenticationSession();
      session.setAuthNote(CURRENT_STEP, EMAIL_STEP);
      resetEmailCode(context);
      showEmailForm(context, null);
      return;
    }

    String enteredCode = formData.getFirst(EmailConstants.CODE);
    if (enteredCode == null || enteredCode.trim().isEmpty()) {
      showOtpForm(context, "Verification code is required");
      return;
    }

    if (validateOtpCode(context, enteredCode)) {
      // Success - complete authentication
      resetEmailCode(context);
      context.success();
    } else {
      showOtpForm(context, "Invalid or expired verification code");
    }
  }

  private void showEmailForm(AuthenticationFlowContext context, String error) {
    LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());

    if (error != null) {
      form.setError(error);
    }

    // Add current email value if available
    AuthenticationSessionModel session = context.getAuthenticationSession();
    String currentEmail = session.getAuthNote(USER_EMAIL);
    if (currentEmail != null) {
      form.setAttribute("email", currentEmail);
    }

    Response response = form.createForm("email-input-form.ftl");
    context.challenge(response);
  }

  private void showOtpForm(AuthenticationFlowContext context, String message) {
    LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());

    if (message != null) {
      if (message.contains("Invalid") || message.contains("expired")) {
        form.setError(message);
      } else {
        form.setInfo(message);
      }
    }

    // Add email to form for display
    AuthenticationSessionModel session = context.getAuthenticationSession();
    String email = session.getAuthNote(USER_EMAIL);
    if (email != null) {
      form.setAttribute("email", email);
    }

    Response response = form.createForm("email-otp-form.ftl");
    context.challenge(response);
  }

  private UserModel findOrCreateUser(AuthenticationFlowContext context, String email) {
    KeycloakSession session = context.getSession();
    RealmModel realm = context.getRealm();
    UserProvider userProvider = session.users();

    // Try to find existing user by email
    UserModel user = userProvider.getUserByEmail(realm, email);

    if (user == null) {
      // Create new user
      log.infof("Creating new user with email: %s", email);
      user = userProvider.addUser(realm, email);
      user.setEmail(email);
      user.setEmailVerified(false); // Will be verified after OTP
      user.setEnabled(true);

      // Set username same as email
      user.setUsername(email);
    }

    return user;
  }

  private void generateAndSendEmailCode(AuthenticationFlowContext context) {
    AuthenticatorConfigModel config = context.getAuthenticatorConfig();
    AuthenticationSessionModel session = context.getAuthenticationSession();

    int length = EmailConstants.DEFAULT_LENGTH;
    int ttl = EmailConstants.DEFAULT_TTL;
    if (config != null) {
      String lengthStr = config.getConfig().get(EmailConstants.CODE_LENGTH);
      String ttlStr = config.getConfig().get(EmailConstants.CODE_TTL);
      if (lengthStr != null) length = Integer.parseInt(lengthStr);
      if (ttlStr != null) ttl = Integer.parseInt(ttlStr);
    }

    String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
    sendEmailWithCode(context.getSession(), context.getRealm(), context.getUser(), code, ttl);
    session.setAuthNote(EmailConstants.CODE, code);
    session.setAuthNote(EmailConstants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
  }

  private boolean validateOtpCode(AuthenticationFlowContext context, String enteredCode) {
    AuthenticationSessionModel session = context.getAuthenticationSession();
    String storedCode = session.getAuthNote(EmailConstants.CODE);
    String ttlStr = session.getAuthNote(EmailConstants.CODE_TTL);

    if (storedCode == null || ttlStr == null) {
      return false;
    }

    // Check if code matches
    if (!enteredCode.equals(storedCode)) {
      return false;
    }

    // Check if code is expired
    long ttl = Long.parseLong(ttlStr);
    if (System.currentTimeMillis() > ttl) {
      return false;
    }

    // Mark email as verified for new users
    UserModel user = context.getUser();
    if (!user.isEmailVerified()) {
      user.setEmailVerified(true);
    }

    return true;
  }

  private void resetEmailCode(AuthenticationFlowContext context) {
    AuthenticationSessionModel session = context.getAuthenticationSession();
    session.removeAuthNote(EmailConstants.CODE);
    session.removeAuthNote(EmailConstants.CODE_TTL);
  }

  private boolean isValidEmail(String email) {
    return email.contains("@") && email.contains(".");
  }

  private void sendEmailWithCode(KeycloakSession session, RealmModel realm, UserModel user, String code, int ttl) {
    if (user.getEmail() == null) {
      log.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
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
      emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
    } catch (EmailException eex) {
      log.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
    }
  }

  @Override
  public boolean requiresUser() {
    return false; // We handle user creation ourselves
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true; // Always available
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    // NOOP
  }

  @Override
  public void close() {
    // NOOP
  }
}