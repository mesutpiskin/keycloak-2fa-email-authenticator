package com.mesutpiskin.keycloak.auth.email;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class EmailAuthenticatorCredentialProvider
        implements CredentialProvider<EmailAuthenticatorCredentialModel>, CredentialInputValidator {

    private final KeycloakSession session;
    private static final Logger logger = Logger.getLogger(EmailAuthenticatorCredentialProvider.class);

    public EmailAuthenticatorCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        return false; // Not used for validation in this context
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) {
            return false;
        }
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user,
            EmailAuthenticatorCredentialModel credentialModel) {
        if (EmailAuthenticatorCredentialModel.ensureMetadata(credentialModel)) {
            logger.debugf("Initialized email authenticator credential metadata for user %s", user.getId());
        }
        if (credentialModel.getUserLabel() == null || credentialModel.getUserLabel().isBlank()) {
            String label = user.getEmail();
            credentialModel.setUserLabel(label == null || label.isBlank() ? "Email OTP" : label);
        }
        return user.credentialManager().createStoredCredential(credentialModel);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public EmailAuthenticatorCredentialModel getCredentialFromModel(CredentialModel model) {
        return EmailAuthenticatorCredentialModel.createFromCredentialModel(model);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext context) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName("email-authenticator-display-name")
                .helpText("email-authenticator-help-text")
                .iconCssClass("kcAuthenticatorEmailClass")
                .createAction(EmailAuthenticatorRequiredAction.PROVIDER_ID)
                .removeable(true)
                .build(session);
    }

    @Override
    public String getType() {
        return EmailAuthenticatorCredentialModel.TYPE_ID;
    }
}