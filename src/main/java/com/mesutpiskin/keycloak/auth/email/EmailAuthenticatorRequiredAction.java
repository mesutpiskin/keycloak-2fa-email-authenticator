package com.mesutpiskin.keycloak.auth.email;

import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class EmailAuthenticatorRequiredAction implements RequiredActionProvider, CredentialRegistrator {

    public static final String PROVIDER_ID = "email-authenticator-setup";
    private static final String SETUP_TEMPLATE = "email-authenticator-setup-form.ftl";
    private static final Logger logger = Logger.getLogger(EmailAuthenticatorRequiredAction.class);

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
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSessionModel) {
        return EmailAuthenticatorCredentialModel.TYPE_ID;
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

}