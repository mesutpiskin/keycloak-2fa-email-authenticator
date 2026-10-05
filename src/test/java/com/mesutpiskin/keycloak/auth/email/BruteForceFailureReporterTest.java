package com.mesutpiskin.keycloak.auth.email;

import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@code BruteForceProtector.failedLogin} changed signature in 26.6 and again
 * in 26.7. These tests pin down that each shape is handled, and that an older
 * Keycloak without a category-aware variant is skipped instead of failing the
 * login.
 */
@DisplayName("BruteForceFailureReporter")
class BruteForceFailureReporterTest {

    /** The 26.7+ shape. */
    interface SetProtector {
        void failedLogin(RealmModel realm, UserModel user, ClientConnection connection, UriInfo uriInfo,
                Set<String> categories);
    }

    /** The 26.6 shape. */
    interface StringProtector {
        void failedLogin(RealmModel realm, UserModel user, ClientConnection connection, UriInfo uriInfo,
                String category);
    }

    /** The 26.5 and earlier shape: no category at all. */
    interface LegacyProtector {
        void failedLogin(RealmModel realm, UserModel user, ClientConnection connection, UriInfo uriInfo);
    }

    private final RealmModel realm = mock(RealmModel.class);
    private final UserModel user = mock(UserModel.class);
    private final ClientConnection connection = mock(ClientConnection.class);
    private final UriInfo uriInfo = mock(UriInfo.class);

    @Test
    @DisplayName("Resolves the Set<String> variant (Keycloak 26.7+) and reports under 'otp'")
    void testResolvesSetVariant() throws Exception {
        SetProtector protector = mock(SetProtector.class);

        BruteForceFailureReporter.Invoker invoker = BruteForceFailureReporter.resolve(SetProtector.class);

        assertNotNull(invoker);
        invoker.failedLogin(protector, realm, user, connection, uriInfo);
        verify(protector).failedLogin(realm, user, connection, uriInfo, Set.of("otp"));
    }

    @Test
    @DisplayName("Falls back to the String variant (Keycloak 26.6) and reports under 'otp'")
    void testResolvesStringVariant() throws Exception {
        StringProtector protector = mock(StringProtector.class);

        BruteForceFailureReporter.Invoker invoker = BruteForceFailureReporter.resolve(StringProtector.class);

        assertNotNull(invoker);
        invoker.failedLogin(protector, realm, user, connection, uriInfo);
        verify(protector).failedLogin(realm, user, connection, uriInfo, "otp");
    }

    @Test
    @DisplayName("Resolves nothing when there is no category-aware variant (Keycloak 26.5 and earlier)")
    void testResolvesNothingWithoutCategoryVariant() {
        assertNull(BruteForceFailureReporter.resolve(LegacyProtector.class));
    }

    @Test
    @DisplayName("The Keycloak version this build targets has a category-aware variant")
    void testResolvesAgainstTheBuildTarget() {
        assertNotNull(BruteForceFailureReporter.resolve(BruteForceProtector.class));
    }

    @Test
    @DisplayName("Without a resolved variant the report is skipped and nothing is touched")
    void testReportIsSkippedWithoutInvoker() {
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);

        assertDoesNotThrow(() -> BruteForceFailureReporter.report(null, context, user));

        verifyNoInteractions(context);
    }

    @Test
    @DisplayName("A failing protector never fails the login")
    void testProtectorFailureIsSwallowed() {
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        KeycloakSession session = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(session);
        when(session.getProvider(BruteForceProtector.class)).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> BruteForceFailureReporter.report(
                BruteForceFailureReporter.resolve(SetProtector.class), context, user));
    }
}
