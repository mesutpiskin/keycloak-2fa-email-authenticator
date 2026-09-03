package com.mesutpiskin.keycloak.auth.email;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

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
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> properties = new ArrayList<>(MAX_AUTH_AGE_CONFIG_PROPERTIES);
        properties.addAll(EmailAuthenticatorFormFactory.SENDING_CONFIG_PROPERTIES);
        properties.add(new ProviderConfigProperty(
                EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM,
                "Show Masked Email on OTP Form",
                "If enabled, displays a masked version of the user's email address on the enrollment verification form after the code is sent.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                String.valueOf(EmailConstants.DEFAULT_SHOW_MASKED_EMAIL_ON_OTP_FORM)));
        return properties;
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