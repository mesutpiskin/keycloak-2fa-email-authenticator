package com.mesutpiskin.keycloak.auth.email;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

@AutoService(AuthenticatorFactory.class)
public class EmailAuthenticatorFormFactory implements AuthenticatorFactory {
    @Override
    public String getId() {
        return "email-authenticator";
    }

    @Override
    public String getDisplayType() {
        return "Email OTP";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Email otp authenticator.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(EmailConstants.CODE_LENGTH, "Code length",
                        "The number of digits of the generated code.",
                        ProviderConfigProperty.STRING_TYPE, String.valueOf(EmailConstants.DEFAULT_LENGTH)),
                new ProviderConfigProperty(EmailConstants.CODE_TTL, "Time-to-live",
                        "The time to live in seconds for the code to be valid.", ProviderConfigProperty.STRING_TYPE,
                        String.valueOf(EmailConstants.DEFAULT_TTL)));
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new EmailAuthenticatorForm(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }
}
