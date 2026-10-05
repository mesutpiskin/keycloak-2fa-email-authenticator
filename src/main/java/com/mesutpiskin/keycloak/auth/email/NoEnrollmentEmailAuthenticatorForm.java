package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Email OTP authenticator that needs no enrolment
 * ({@code email-authenticator-no-enrollment}): every user with an email
 * address is eligible.
 * <p>
 * Use it when an admin wants email OTP for every user, for example
 * admin-provisioned accounts, or wants it offered as a "Try Another Way"
 * alternative to users who never enrolled.
 * </p>
 * <p>
 * <strong>Do not implement</strong>
 * {@link org.keycloak.authentication.CredentialValidator CredentialValidator}
 * here. That interface is exactly what makes Keycloak hide an authenticator
 * from "Try Another Way" for users without a stored credential (issue #147).
 * Because this class does not implement it, Keycloak treats it as a
 * non-credential authenticator:
 * </p>
 * <ul>
 * <li>It is always listed under "Try Another Way", after the user's
 * credential-based options. Keycloak does not check {@link #configuredFor} when
 * building that list, so a user <em>without</em> an email also sees the option.
 * Choosing it ends with Keycloak's "credential setup required" error page.</li>
 * <li>"Condition - User Configured" fires for every user with an email.</li>
 * <li>The list entry's label comes from the
 * {@code email-authenticator-no-enrollment-display-name} and
 * {@code email-authenticator-no-enrollment-help-text} message keys. Keycloak
 * uses the default icon.</li>
 * </ul>
 * <p>
 * Existing {@code email-authenticator} credentials are ignored. A user with a
 * stored credential but no email address is not eligible.
 * </p>
 *
 * @since 26.7.0
 * @see EmailAuthenticatorForm
 * @see NoEnrollmentEmailAuthenticatorFormFactory
 */
public class NoEnrollmentEmailAuthenticatorForm extends AbstractEmailAuthenticatorForm {

    /**
     * A user is configured whenever they have an email address.
     */
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return hasEmail(user);
    }
}
