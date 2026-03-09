package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailAuthenticatorCredentialProviderTest {

    private EmailAuthenticatorCredentialProvider provider;
    private KeycloakSession session;
    private RealmModel realm;
    private UserModel user;
    private SubjectCredentialManager credentialManager;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        credentialManager = mock(SubjectCredentialManager.class);
        when(user.credentialManager()).thenReturn(credentialManager);
        provider = new EmailAuthenticatorCredentialProvider(session);
    }

    @Test
    void testIsConfiguredFor_WithStoredCredential() {
        var credential = new EmailAuthenticatorCredentialModel();
        when(credentialManager.getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID))
                .thenReturn(Stream.of(credential));

        boolean result = provider.isConfiguredFor(realm, user, EmailAuthenticatorCredentialModel.TYPE_ID);

        assertTrue(result, "Should be configured when a stored credential exists");
    }

    @Test
    void testIsConfiguredFor_WithNoStoredCredential() {
        when(credentialManager.getStoredCredentialsByTypeStream(EmailAuthenticatorCredentialModel.TYPE_ID))
                .thenReturn(Stream.empty());

        boolean result = provider.isConfiguredFor(realm, user, EmailAuthenticatorCredentialModel.TYPE_ID);

        assertFalse(result, "Should not be configured when no stored credential exists");
    }

    @Test
    void testIsConfiguredFor_WrongCredentialType() {
        boolean result = provider.isConfiguredFor(realm, user, "wrong-type");

        assertFalse(result, "Should return false for unsupported credential type");
    }
}
