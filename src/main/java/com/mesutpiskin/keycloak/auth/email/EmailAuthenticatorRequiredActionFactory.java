package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class EmailAuthenticatorRequiredActionFactory implements RequiredActionFactory {

    private static final EmailAuthenticatorRequiredAction SINGLETON = new EmailAuthenticatorRequiredAction();

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return EmailAuthenticatorRequiredAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Set up Email Authenticator";
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No configuration needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post init needed
    }

    @Override
    public void close() {
        // No resources to close
    }
}