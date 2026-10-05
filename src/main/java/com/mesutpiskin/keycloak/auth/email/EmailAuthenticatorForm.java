package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.List;

/**
 * Credential-based email OTP authenticator ({@code email-authenticator}): a
 * user is eligible only after enrolling an {@code email-authenticator}
 * credential.
 * <p>
 * This follows Keycloak's convention for built-in second factors such as OTP and
 * WebAuthn:
 * </p>
 * <ul>
 * <li>"Condition - User Configured" only fires for users who have actually
 * enrolled.</li>
 * <li>Because it implements {@link CredentialValidator}, Keycloak lists it
 * under "Try Another Way" only for users with a stored credential, ordered by
 * the user's credential priority.</li>
 * <li>The account console offers enrolment through
 * {@link EmailAuthenticatorRequiredAction}.</li>
 * </ul>
 * <p>
 * To offer email OTP to every user with an email address without enrolment,
 * use {@link NoEnrollmentEmailAuthenticatorForm} instead.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.1.1
 * @since 1.0.0
 * @see AbstractEmailAuthenticatorForm
 */
public class EmailAuthenticatorForm extends AbstractEmailAuthenticatorForm
        implements CredentialValidator<EmailAuthenticatorCredentialProvider> {

    /**
     * A user is configured when they have an email address <em>and</em> a stored
     * {@link EmailAuthenticatorCredentialModel#TYPE_ID email-authenticator}
     * credential.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return hasEmail(user) && hasStoredEmailCredential(user);
    }

    private static boolean hasStoredEmailCredential(UserModel user) {
        SubjectCredentialManager credentialManager = user.credentialManager();
        if (credentialManager == null) {
            return false;
        }
        return credentialManager
                .getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID)
                .findAny()
                .isPresent();
    }

    @Override
    public EmailAuthenticatorCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (EmailAuthenticatorCredentialProvider) session.getProvider(CredentialProvider.class,
                EmailAuthenticatorCredentialProviderFactory.PROVIDER_ID);
    }

    /**
     * Declares the enrolment required action so Keycloak and the account console
     * know how this credential is set up. It is never assigned automatically
     * (see {@link #setRequiredActions}).
     */
    @Override
    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {
        return Collections.singletonList((EmailAuthenticatorRequiredActionFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RequiredActionProvider.class, EmailAuthenticatorRequiredAction.PROVIDER_ID));
    }
}
