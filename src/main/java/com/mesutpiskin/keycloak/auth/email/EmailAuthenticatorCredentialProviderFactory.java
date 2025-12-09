package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

public class EmailAuthenticatorCredentialProviderFactory
        implements CredentialProviderFactory<EmailAuthenticatorCredentialProvider> {
    public static final String PROVIDER_ID = EmailAuthenticatorCredentialModel.TYPE_ID;

    @Override
    public EmailAuthenticatorCredentialProvider create(KeycloakSession session) {
        return new EmailAuthenticatorCredentialProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}