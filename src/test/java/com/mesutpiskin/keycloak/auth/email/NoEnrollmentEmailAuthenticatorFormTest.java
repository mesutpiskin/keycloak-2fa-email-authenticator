package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NoEnrollmentEmailAuthenticatorForm}.
 */
@DisplayName("NoEnrollmentEmailAuthenticatorForm Tests")
class NoEnrollmentEmailAuthenticatorFormTest {

    private NoEnrollmentEmailAuthenticatorForm authenticator;
    private KeycloakSession session;
    private RealmModel realm;

    @BeforeEach
    void setUp() {
        authenticator = new NoEnrollmentEmailAuthenticatorForm();
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
    }

    @Test
    @DisplayName("Is not a CredentialValidator — otherwise Keycloak hides it from 'Try Another Way' for non-enrolled users (issue #147)")
    void testIsNotCredentialValidator() {
        assertFalse(authenticator instanceof CredentialValidator,
                "Implementing CredentialValidator makes Keycloak list this option only for users with a stored credential");
    }

    @Test
    @DisplayName("configuredFor returns true for a user with an email and no stored credential, without touching credentials or the realm")
    void testConfiguredFor_userWithEmail_returnsTrue() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("alice@example.com");

        assertTrue(authenticator.configuredFor(session, realm, user));
        verify(user, never()).credentialManager();
        verifyNoInteractions(realm);
    }

    @Test
    @DisplayName("configuredFor returns false when the user has no email")
    void testConfiguredFor_noEmail_returnsFalse() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn(null);

        assertFalse(authenticator.configuredFor(session, realm, user),
                "Nowhere to send the code");
    }

    @Test
    @DisplayName("configuredFor returns false for a blank email")
    void testConfiguredFor_blankEmail_returnsFalse() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("  ");

        assertFalse(authenticator.configuredFor(session, realm, user));
    }

    @Test
    @DisplayName("configuredFor returns false for a null user")
    void testConfiguredFor_nullUser_returnsFalse() {
        assertFalse(authenticator.configuredFor(session, realm, null));
    }

    @Test
    @DisplayName("Requires a user and declares no required actions")
    void testRequiresUser_noRequiredActions() {
        assertTrue(authenticator.requiresUser());
        assertTrue(authenticator.getRequiredActions(session).isEmpty(),
                "There is nothing to enrol, so no required action may be advertised");
    }

    @Test
    @DisplayName("setRequiredActions never assigns an action")
    void testSetRequiredActions_isNoOp() {
        UserModel user = mock(UserModel.class);

        authenticator.setRequiredActions(session, realm, user);

        verifyNoInteractions(user);
    }
}
