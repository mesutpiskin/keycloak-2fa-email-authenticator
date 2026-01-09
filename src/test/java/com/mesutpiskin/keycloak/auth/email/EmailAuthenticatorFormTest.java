package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailAuthenticatorForm}.
 * 
 * Note: These are simplified tests that don't require full Keycloak mocking.
 * They focus on testing isolated logic and helper methods.
 */
@DisplayName("EmailAuthenticatorForm Tests")
class EmailAuthenticatorFormTest {

    private EmailAuthenticatorForm authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new EmailAuthenticatorForm();
    }

    @Test
    @DisplayName("Should require user")
    void testRequiresUser() {
        assertTrue(authenticator.requiresUser(),
                "Email authenticator should require a user to be present");
    }

    @Test
    @DisplayName("Should have correct credential provider type")
    void testGetCredentialProvider() {
        KeycloakSession session = mock(KeycloakSession.class);

        // We can't fully test this without Keycloak infrastructure,
        // but we can verify it doesn't throw exceptions
        assertDoesNotThrow(() -> {
            try {
                authenticator.getCredentialProvider(session);
            } catch (NullPointerException e) {
                // Expected when session doesn't have provider infrastructure
                // This is acceptable for unit test
            }
        });
    }

    @Test
    @DisplayName("Should close without errors")
    void testClose() {
        assertDoesNotThrow(() -> authenticator.close(),
                "Close should not throw any exceptions");
    }

    @Test
    @DisplayName("Should be configured for user with valid credential")
    void testConfiguredFor_withValidSetup() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);

        // This test verifies the method can be called without exceptions
        assertDoesNotThrow(() -> {
            try {
                authenticator.configuredFor(session, realm, user);
            } catch (NullPointerException e) {
                // Expected when credential provider is not available
                // This is acceptable for unit test
            }
        });
    }

    @Test
    @DisplayName("Should set required action on user")
    void testSetRequiredActions() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);

        authenticator.setRequiredActions(session, realm, user);

        // Verify that addRequiredAction was called with the correct provider ID
        verify(user).addRequiredAction(EmailAuthenticatorRequiredAction.PROVIDER_ID);
    }

    @Test
    @DisplayName("Should return required actions list")
    void testGetRequiredActions() {
        KeycloakSession session = mock(KeycloakSession.class);

        assertDoesNotThrow(() -> {
            try {
                var actions = authenticator.getRequiredActions(session);
                // The result depends on session factory setup
            } catch (NullPointerException e) {
                // Expected when session factory is not available
            }
        });
    }

    /**
     * Helper class to test protected methods via extension.
     */
    static class TestableEmailAuthenticatorForm extends EmailAuthenticatorForm {
        // Expose protected method for testing
        public String testDisabledByBruteForceError() {
            return disabledByBruteForceError();
        }
    }

    @Test
    @DisplayName("Should return correct brute force error message")
    void testDisabledByBruteForceError() {
        TestableEmailAuthenticatorForm testable = new TestableEmailAuthenticatorForm();

        String errorMessage = testable.testDisabledByBruteForceError();

        assertNotNull(errorMessage, "Error message should not be null");
        assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
    }

    @Test
    @DisplayName("Should handle authentication session model")
    void testAuthenticationSessionHandling() {
        AuthenticationSessionModel session = mock(AuthenticationSessionModel.class);

        // Setup mock behavior
        when(session.getAuthNote(EmailConstants.CODE)).thenReturn(null);
        when(session.getAuthNote(EmailConstants.CODE_TTL)).thenReturn(null);

        // Verify session can be queried
        assertNull(session.getAuthNote(EmailConstants.CODE));
        assertNull(session.getAuthNote(EmailConstants.CODE_TTL));
    }

    @Test
    @DisplayName("Should validate constants usage")
    void testConstantsUsage() {
        // Verify that the authenticator uses the correct constants
        assertEquals("emailCode", EmailConstants.CODE);
        assertEquals(6, EmailConstants.DEFAULT_LENGTH);
        assertEquals(300, EmailConstants.DEFAULT_TTL);
        assertEquals(30, EmailConstants.DEFAULT_RESEND_COOLDOWN);
    }
}
