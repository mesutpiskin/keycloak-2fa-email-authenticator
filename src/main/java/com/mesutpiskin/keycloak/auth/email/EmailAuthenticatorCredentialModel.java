package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;

public class EmailAuthenticatorCredentialModel extends CredentialModel {
    public static final String TYPE_ID = "email-authenticator";
    private static final String DEFAULT_CREDENTIAL_DATA = "{\"type\":\"" + TYPE_ID + "\",\"version\":1}";

    public static EmailAuthenticatorCredentialModel create() {
        EmailAuthenticatorCredentialModel model = new EmailAuthenticatorCredentialModel();
        ensureMetadata(model);
        return model;
    }

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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}