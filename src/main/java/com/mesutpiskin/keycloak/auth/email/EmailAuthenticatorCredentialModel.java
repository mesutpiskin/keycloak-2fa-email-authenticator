package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;

/**
 * Credential model for email-based two-factor authentication.
 * <p>
 * This class extends Keycloak's {@link CredentialModel} to represent the email
 * authenticator
 * credential type. It provides factory methods for creating and managing
 * credential instances,
 * ensuring proper metadata initialization.
 * </p>
 * <p>
 * The credential model stores metadata about the email authenticator setup but
 * does not
 * store the actual OTP codes (which are session-based and short-lived).
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
 * @since 1.0.0
 */
public class EmailAuthenticatorCredentialModel extends CredentialModel {

    /**
     * Type identifier for the email authenticator credential.
     */
    public static final String TYPE_ID = "email-authenticator";

    /**
     * Default JSON credential data with type and version information.
     */
    private static final String DEFAULT_CREDENTIAL_DATA = "{\"type\":\"" + TYPE_ID + "\",\"version\":1}";

    /**
     * Creates a new email authenticator credential model with default metadata.
     * <p>
     * The created model has the type set to {@link #TYPE_ID}, creation date set to
     * current time, and default credential data.
     * </p>
     *
     * @return a new credential model instance with proper metadata
     */
    public static EmailAuthenticatorCredentialModel create() {
        EmailAuthenticatorCredentialModel model = new EmailAuthenticatorCredentialModel();
        ensureMetadata(model);
        return model;
    }

    /**
     * Creates an email authenticator credential model from an existing credential
     * model.
     * <p>
     * Copies all properties from the source model and ensures metadata is properly
     * set.
     * Useful when converting a generic {@link CredentialModel} to this specific
     * type.
     * </p>
     *
     * @param model the source credential model to copy from
     * @return a new email authenticator credential model with copied properties
     */
    public static EmailAuthenticatorCredentialModel createFromCredentialModel(CredentialModel model) {
        EmailAuthenticatorCredentialModel credentialModel = new EmailAuthenticatorCredentialModel();
        credentialModel.setId(model.getId());
        credentialModel.setType(model.getType());
        credentialModel.setCreatedDate(model.getCreatedDate());
        credentialModel.setUserLabel(model.getUserLabel());
        credentialModel.setCredentialData(model.getCredentialData());
        credentialModel.setSecretData(model.getSecretData());
        ensureMetadata(credentialModel);
        return credentialModel;
    }

    /**
     * Ensures the credential model has proper metadata set.
     * <p>
     * Validates and corrects the following properties if missing or incorrect:
     * <ul>
     * <li>Type: set to {@link #TYPE_ID}</li>
     * <li>Created date: set to current timestamp if null</li>
     * <li>Credential data: set to {@link #DEFAULT_CREDENTIAL_DATA} if blank</li>
     * </ul>
     * </p>
     *
     * @param model the credential model to validate and update
     * @return true if any metadata was updated, false if all metadata was already
     *         correct
     */
    public static boolean ensureMetadata(CredentialModel model) {
        boolean updated = false;
        if (model == null) {
            return false;
        }
        if (!TYPE_ID.equals(model.getType())) {
            model.setType(TYPE_ID);
            updated = true;
        }
        if (model.getCreatedDate() == null) {
            model.setCreatedDate(Time.currentTimeMillis());
            updated = true;
        }
        if (isBlank(model.getCredentialData())) {
            model.setCredentialData(DEFAULT_CREDENTIAL_DATA);
            updated = true;
        }
        return updated;
    }

    /**
     * Checks if a string value is null, empty, or contains only whitespace.
     *
     * @param value the string to check
     * @return true if the value is blank, false otherwise
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}