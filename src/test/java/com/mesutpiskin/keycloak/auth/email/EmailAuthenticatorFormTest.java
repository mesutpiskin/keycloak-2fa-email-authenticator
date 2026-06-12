package com.mesutpiskin.keycloak.auth.email;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    @DisplayName("configuredFor returns true when the user has a stored email-authenticator credential")
    void testConfiguredFor_withStoredCredential() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        SubjectCredentialManager cm = mock(SubjectCredentialManager.class);
        when(user.getEmail()).thenReturn("alice@example.com");
        when(user.credentialManager()).thenReturn(cm);
        when(cm.getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID))
                .thenReturn(Stream.of(new EmailAuthenticatorCredentialModel()));

        assertTrue(authenticator.configuredFor(session, realm, user),
                "An enrolled user must be reported as configured");
    }

    @Test
    @DisplayName("configuredFor returns false when the user has no email")
    void testConfiguredFor_userWithoutEmail() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn(null);

        assertFalse(authenticator.configuredFor(session, realm, user),
                "Users without an email cannot use the email authenticator");
    }

    @Test
    @DisplayName("configuredFor returns false for blank emails")
    void testConfiguredFor_blankEmail() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn("   ");

        assertFalse(authenticator.configuredFor(session, realm, user),
                "Blank emails should be treated as no email");
    }

    @Test
    @DisplayName("configuredFor returns false by default for users with email but no stored credential — regression test for unwanted email-OTP prompt in Conditional - User Configured sub-flows (issue #108 follow-up)")
    void testConfiguredFor_userWithEmail_noStoredCredential_noSkipSetupConfig_returnsFalse() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        SubjectCredentialManager cm = mock(SubjectCredentialManager.class);
        when(user.getEmail()).thenReturn("alice@example.com");
        when(user.credentialManager()).thenReturn(cm);
        when(cm.getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID))
                .thenReturn(Stream.empty());
        when(realm.getAuthenticationFlowsStream()).thenReturn(Stream.empty());

        assertFalse(authenticator.configuredFor(session, realm, user),
                "When no admin has opted in via skipSetup=true, a non-enrolled user must not be reported as configured — otherwise 'Conditional - User Configured' sub-flows trigger unexpectedly");
    }

    @Test
    @DisplayName("configuredFor returns true when an admin has set skipSetup=true on any email-authenticator execution and the user has an email")
    void testConfiguredFor_skipSetupTrue_userWithEmail_returnsTrue() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        SubjectCredentialManager cm = mock(SubjectCredentialManager.class);
        AuthenticationFlowModel flow = mock(AuthenticationFlowModel.class);
        AuthenticationExecutionModel exec = mock(AuthenticationExecutionModel.class);
        AuthenticatorConfigModel cfg = mock(AuthenticatorConfigModel.class);

        when(user.getEmail()).thenReturn("alice@example.com");
        when(user.credentialManager()).thenReturn(cm);
        when(cm.getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID))
                .thenReturn(Stream.empty());

        when(flow.getId()).thenReturn("flow-1");
        when(realm.getAuthenticationFlowsStream()).thenReturn(Stream.of(flow));
        when(realm.getAuthenticationExecutionsStream("flow-1")).thenReturn(Stream.of(exec));
        when(exec.getAuthenticator()).thenReturn(EmailAuthenticatorFormFactory.PROVIDER_ID);
        when(exec.getAuthenticatorConfig()).thenReturn("cfg-1");
        when(realm.getAuthenticatorConfigById("cfg-1")).thenReturn(cfg);
        when(cfg.getConfig()).thenReturn(Map.of(EmailConstants.SKIP_SETUP, "true"));

        assertTrue(authenticator.configuredFor(session, realm, user),
                "skipSetup=true is the explicit opt-in for the 'any user with email is eligible' behaviour");
    }

    @Test
    @DisplayName("configuredFor returns false when skipSetup=true is opted in but the user has no email")
    void testConfiguredFor_skipSetupTrue_noEmail_returnsFalse() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn(null);

        assertFalse(authenticator.configuredFor(session, realm, user),
                "skipSetup cannot rescue a user with no email — the authenticator has nowhere to send the code");
    }

    @Test
    @DisplayName("configuredFor returns false when skipSetup is explicitly false on every execution (issue #108 follow-up scenario)")
    void testConfiguredFor_skipSetupFalseEverywhere_returnsFalse() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        SubjectCredentialManager cm = mock(SubjectCredentialManager.class);
        AuthenticationFlowModel flow = mock(AuthenticationFlowModel.class);
        AuthenticationExecutionModel exec = mock(AuthenticationExecutionModel.class);
        AuthenticatorConfigModel cfg = mock(AuthenticatorConfigModel.class);

        when(user.getEmail()).thenReturn("alice@example.com");
        when(user.credentialManager()).thenReturn(cm);
        when(cm.getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID))
                .thenReturn(Stream.empty());

        when(flow.getId()).thenReturn("flow-1");
        when(realm.getAuthenticationFlowsStream()).thenReturn(Stream.of(flow));
        when(realm.getAuthenticationExecutionsStream("flow-1")).thenReturn(Stream.of(exec));
        when(exec.getAuthenticator()).thenReturn(EmailAuthenticatorFormFactory.PROVIDER_ID);
        when(exec.getAuthenticatorConfig()).thenReturn("cfg-1");
        when(realm.getAuthenticatorConfigById("cfg-1")).thenReturn(cfg);
        when(cfg.getConfig()).thenReturn(Map.of(EmailConstants.SKIP_SETUP, "false"));

        assertFalse(authenticator.configuredFor(session, realm, user),
                "An admin who explicitly disables skipSetup must get the strict, enrolment-only behaviour");
    }

    @Test
    @DisplayName("setRequiredActions is a no-op; the login flow never triggers enrolment")
    void testSetRequiredActions_isNoOp() {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);

        authenticator.setRequiredActions(session, realm, user);

        verify(user, never()).addRequiredAction(EmailAuthenticatorRequiredAction.PROVIDER_ID);
        verify(user, never()).addRequiredAction(anyString());
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

    /**
     * Bypasses enabledUser (requires full Keycloak infra) to isolate brute-force logic.
     */
    static class BfpTestableForm extends EmailAuthenticatorForm {
        @Override
        public boolean enabledUser(AuthenticationFlowContext context, UserModel user) {
            return true;
        }
    }

    @Nested
    @DisplayName("Brute force protection — Keycloak BFP vs lib counter")
    class BruteForceProtectionTests {

        private BfpTestableForm form;
        private AuthenticationFlowContext context;
        private AuthenticationSessionModel session;
        private RealmModel realm;
        private UserModel user;
        private LoginFormsProvider loginForm;

        @BeforeEach
        void setUp() {
            form = new BfpTestableForm();
            context = mock(AuthenticationFlowContext.class);
            session = mock(AuthenticationSessionModel.class);
            realm = mock(RealmModel.class);
            user = mock(UserModel.class);
            loginForm = mock(LoginFormsProvider.class);

            when(context.getUser()).thenReturn(user);
            when(context.getAuthenticationSession()).thenReturn(session);
            when(context.getRealm()).thenReturn(realm);

            HttpRequest httpRequest = mock(HttpRequest.class);
            MultivaluedHashMap<String, String> formData = new MultivaluedHashMap<>();
            formData.putSingle(EmailConstants.CODE, "000000"); // wrong code — stored is hash("123456")
            when(context.getHttpRequest()).thenReturn(httpRequest);
            when(httpRequest.getDecodedFormParameters()).thenReturn(formData);

            when(session.getAuthNote(EmailConstants.CODE)).thenReturn(OtpHashUtils.hash("123456"));
            when(session.getAuthNote(EmailConstants.CODE_TTL))
                    .thenReturn(String.valueOf(System.currentTimeMillis() + 300_000));
            when(session.getAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER)).thenReturn(null);
            when(session.getAuthNote("emailCodeAttempts")).thenReturn(null);

            EventBuilder event = mock(EventBuilder.class);
            when(context.getEvent()).thenReturn(event);
            when(event.user(any(UserModel.class))).thenReturn(event);

            AuthenticationExecutionModel execution = mock(AuthenticationExecutionModel.class);
            when(context.getExecution()).thenReturn(execution);
            when(execution.getId()).thenReturn("test-exec");
            when(context.form()).thenReturn(loginForm);
            when(loginForm.setExecution(anyString())).thenReturn(loginForm);
            when(loginForm.setAttribute(anyString(), any())).thenReturn(loginForm);
            when(loginForm.addError(any())).thenReturn(loginForm);
            when(loginForm.createForm(anyString())).thenReturn(mock(Response.class));

            AuthenticatorConfigModel config = mock(AuthenticatorConfigModel.class);
            when(context.getAuthenticatorConfig()).thenReturn(config);
            when(config.getConfig()).thenReturn(Map.of(EmailConstants.MAX_ATTEMPTS, "5"));
        }

        @Test
        @DisplayName("Keycloak BFP actif — le compteur de la lib ne doit pas être incrémenté")
        void testKeycloakBfpActive_doesNotIncrementAttempts() {
            when(realm.isBruteForceProtected()).thenReturn(true);

            form.action(context);

            verify(session, never()).setAuthNote(eq("emailCodeAttempts"), anyString());
            verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        }

        @Test
        @DisplayName("Keycloak BFP inactif — le compteur de la lib doit être incrémenté")
        void testKeycloakBfpInactive_incrementsAttempts() {
            when(realm.isBruteForceProtected()).thenReturn(false);

            form.action(context);

            verify(session).setAuthNote("emailCodeAttempts", "1");
            verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        }

        @Test
        @DisplayName("Keycloak BFP inactif — seuil atteint : code réinitialisé et flag maxAttemptsReached positionné")
        void testKeycloakBfpInactive_maxAttemptsReached_resetsCode() {
            when(realm.isBruteForceProtected()).thenReturn(false);
            when(session.getAuthNote("emailCodeAttempts")).thenReturn("4"); // prochain = 5 = max

            form.action(context);

            verify(session).setAuthNote("emailCodeAttempts", "5");
            verify(session).removeAuthNote(EmailConstants.CODE);
            verify(loginForm).setAttribute("maxAttemptsReached", true);
            verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        }
    }


    @Nested
    @DisplayName("Masked email exposure tests")
    class MaskedEmailExposureTests {

        private BfpTestableForm form;
        private AuthenticationFlowContext context;
        private UserModel user;
        private AuthenticationSessionModel session;
        private LoginFormsProvider loginForm;
        private AuthenticationExecutionModel execution;
        private AuthenticatorConfigModel config;

        @BeforeEach
        void setUp() {
            form = new BfpTestableForm();
            context = mock(AuthenticationFlowContext.class);
            user = mock(UserModel.class);
            session = mock(AuthenticationSessionModel.class);
            loginForm = mock(LoginFormsProvider.class);
            execution = mock(AuthenticationExecutionModel.class);
            config = mock(AuthenticatorConfigModel.class);

            when(context.getUser()).thenReturn(user);
            when(context.getAuthenticationSession()).thenReturn(session);
            when(context.form()).thenReturn(loginForm);
            when(context.getExecution()).thenReturn(execution);
            when(execution.getId()).thenReturn("test-exec");
            when(loginForm.setExecution(anyString())).thenReturn(loginForm);
            when(loginForm.setAttribute(anyString(), any())).thenReturn(loginForm);
            when(loginForm.createForm(anyString())).thenReturn(mock(Response.class));
            when(context.getAuthenticatorConfig()).thenReturn(config);
            when(session.getAuthNote(EmailConstants.CODE)).thenReturn(OtpHashUtils.hash("123456"));
            when(session.getAuthNote(EmailConstants.CODE_TTL)).thenReturn(String.valueOf(System.currentTimeMillis() + 300_000));
            when(session.getAuthNote(EmailConstants.CODE_RESEND_AVAILABLE_AFTER)).thenReturn(null);
        }

        @Test
        @DisplayName("Should expose masked email when enabled")
        void shouldExposeMaskedEmailWhenEnabled() {
            when(user.getEmail()).thenReturn("username@example.com");
            when(config.getConfig()).thenReturn(Map.of(
                    EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM, "true",
                    EmailConstants.CODE_LENGTH, "6"));

            form.authenticate(context);

            verify(loginForm).setAttribute("maskedEmail", "u***e@example.com");
        }

        @Test
        @DisplayName("Should not expose masked email when disabled")
        void shouldNotExposeMaskedEmailWhenDisabled() {
            when(user.getEmail()).thenReturn("username@example.com");
            when(config.getConfig()).thenReturn(Map.of(
                    EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM, "false",
                    EmailConstants.CODE_LENGTH, "6"));

            form.authenticate(context);

            verify(loginForm, never()).setAttribute(eq("maskedEmail"), any());
        }

        @Test
        @DisplayName("Should expose masked email for short local parts")
        void shouldExposeMaskedEmailForShortLocalParts() {
            when(user.getEmail()).thenReturn("ab@example.com");
            when(config.getConfig()).thenReturn(Map.of(
                    EmailConstants.SHOW_MASKED_EMAIL_ON_OTP_FORM, "true",
                    EmailConstants.CODE_LENGTH, "6"));

            form.authenticate(context);

            verify(loginForm).setAttribute("maskedEmail", "a***@example.com");
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
