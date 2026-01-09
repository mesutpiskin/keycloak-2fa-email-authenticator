package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.keycloak.credential.CredentialModel;

/**
 * Unit tests for {@link EmailAuthenticatorCredentialModel}.
 */
@DisplayName("EmailAuthenticatorCredentialModel Tests")
class EmailAuthenticatorCredentialModelTest {

    @Test
    @DisplayName("Should create new credential model with correct type")
    void testCreate() {
        EmailAuthenticatorCredentialModel model = EmailAuthenticatorCredentialModel.create();

        assertNotNull(model, "Created model should not be null");
        assertEquals(EmailAuthenticatorCredentialModel.TYPE_ID, model.getType(),
                "Type should be email-authenticator");
        assertNotNull(model.getCreatedDate(), "Created date should be set");
        assertNotNull(model.getCredentialData(), "Credential data should be set");
        assertTrue(model.getCredentialData().contains("\"type\":\"email-authenticator\""),
                "Credential data should contain type");
    }

    @Test
    @DisplayName("Should create from existing credential model")
    void testCreateFromCredentialModel() {
        CredentialModel source = new CredentialModel();
        source.setId("test-id");
        source.setType("some-type");
        source.setUserLabel("Test Label");
        source.setCredentialData("{\"test\":\"data\"}");
        source.setSecretData("{\"secret\":\"value\"}");

        EmailAuthenticatorCredentialModel result = EmailAuthenticatorCredentialModel.createFromCredentialModel(source);

        assertNotNull(result, "Result should not be null");
        assertEquals("test-id", result.getId(), "ID should be copied");
        assertEquals(EmailAuthenticatorCredentialModel.TYPE_ID, result.getType(),
                "Type should be corrected to email-authenticator");
        assertEquals("Test Label", result.getUserLabel(), "User label should be copied");
        assertNotNull(result.getCreatedDate(), "Created date should be set");
    }

    @Test
    @DisplayName("Should ensure metadata sets type correctly")
    void testEnsureMetadata_setsType() {
        CredentialModel model = new CredentialModel();
        model.setType("wrong-type");

        boolean updated = EmailAuthenticatorCredentialModel.ensureMetadata(model);

        assertTrue(updated, "Should return true when metadata was updated");
        assertEquals(EmailAuthenticatorCredentialModel.TYPE_ID, model.getType(),
                "Type should be corrected");
    }

    @Test
    @DisplayName("Should ensure metadata sets created date")
    void testEnsureMetadata_setsCreatedDate() {
        CredentialModel model = new CredentialModel();
        model.setType(EmailAuthenticatorCredentialModel.TYPE_ID);

        boolean updated = EmailAuthenticatorCredentialModel.ensureMetadata(model);

        assertTrue(updated, "Should return true when created date was set");
        assertNotNull(model.getCreatedDate(), "Created date should be set");
    }

    @Test
    @DisplayName("Should ensure metadata sets credential data when blank")
    void testEnsureMetadata_setsCredentialData() {
        CredentialModel model = new CredentialModel();
        model.setType(EmailAuthenticatorCredentialModel.TYPE_ID);
        model.setCreatedDate(System.currentTimeMillis());
        model.setCredentialData("   "); // blank

        boolean updated = EmailAuthenticatorCredentialModel.ensureMetadata(model);

        assertTrue(updated, "Should return true when credential data was set");
        assertNotNull(model.getCredentialData(), "Credential data should not be null");
        assertFalse(model.getCredentialData().trim().isEmpty(),
                "Credential data should not be blank");
        assertTrue(model.getCredentialData().contains("email-authenticator"),
                "Credential data should contain type");
    }

    @Test
    @DisplayName("Should not update when metadata is already correct")
    void testEnsureMetadata_noUpdateNeeded() {
        CredentialModel model = new CredentialModel();
        model.setType(EmailAuthenticatorCredentialModel.TYPE_ID);
        model.setCreatedDate(System.currentTimeMillis());
        model.setCredentialData("{\"type\":\"email-authenticator\",\"version\":1}");

        boolean updated = EmailAuthenticatorCredentialModel.ensureMetadata(model);

        assertFalse(updated, "Should return false when no update was needed");
    }

    @Test
    @DisplayName("Should handle null model gracefully")
    void testEnsureMetadata_nullModel() {
        boolean updated = EmailAuthenticatorCredentialModel.ensureMetadata(null);

        assertFalse(updated, "Should return false for null model");
    }

    @Test
    @DisplayName("Should have correct type constant")
    void testTypeConstant() {
        assertEquals("email-authenticator", EmailAuthenticatorCredentialModel.TYPE_ID,
                "TYPE_ID should match expected value");
    }
}
