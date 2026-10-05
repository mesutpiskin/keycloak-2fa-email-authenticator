package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;

/**
 * Factory for {@link NoEnrollmentEmailAuthenticatorForm}, shown in the admin
 * console as "Email OTP (No Enrollment)".
 * <p>
 * It uses only the shared settings from
 * {@link AbstractEmailAuthenticatorFormFactory}. Enrolment-specific settings
 * are left out because this authenticator never enrols anyone.
 * </p>
 * <p>
 * It also registers the one-shot {@link SkipSetupMigration} that runs after
 * Keycloak's own migrations. The migration lives here because this
 * authenticator replaces the removed {@code skipSetup} option.
 * </p>
 *
 * @since 26.7.0
 */
public class NoEnrollmentEmailAuthenticatorFormFactory extends AbstractEmailAuthenticatorFormFactory {

    public static final String PROVIDER_ID = "email-authenticator-no-enrollment";
    public static final NoEnrollmentEmailAuthenticatorForm SINGLETON = new NoEnrollmentEmailAuthenticatorForm();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Email OTP (No Enrollment)";
    }

    @Override
    public String getHelpText() {
        return "Email OTP for every user with an email address, with no enrollment needed. "
                + "Shown under 'Try Another Way' even to users who never enrolled. Note that "
                + "'Condition - User Configured' is true for every user with an email.";
    }

    /**
     * Returns this authenticator's own id instead of the
     * {@code email-authenticator} credential type. This keeps Keycloak from
     * pairing the execution with stored credentials, and from advertising
     * credential enrolment in the account console just because this execution
     * exists. The value must not be {@code null}: Keycloak calls
     * {@code getReferenceCategory().equals(..)} on sibling executions.
     */
    @Override
    public String getReferenceCategory() {
        return PROVIDER_ID;
    }

    /**
     * There is nothing to set up: if a user has no email, Keycloak rejects the
     * execution instead of assigning a required action.
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof PostMigrationEvent) {
                SkipSetupMigration.migrateAllRealms(factory);
            }
        });
    }
}
