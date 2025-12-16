/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Netzbegruenung e.V.
 * @author verdigado eG
 * @author <a href="mailto:christophe@kyvrakidis.com">Christophe Kyvrakidis</a>
 */

package com.mesutpiskin.keycloak.auth.email;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class EmailRequiredAction implements RequiredActionProvider, CredentialRegistrator {

	public static final String PROVIDER_ID = "email_otp_config";

	private static final Logger logger = Logger.getLogger(EmailRequiredAction.class);

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("email-2fa");
        if (config == null) {
            logger.error("Failed to get Email 2FA enforcement configuration");
            return;
        }
        boolean forceSecondFactorEnabled = Boolean.parseBoolean(config.getConfig().get("forceSecondFactor"));
        if (forceSecondFactorEnabled) {

            // list of accepted 2FA alternatives
            List<String> secondFactors = Arrays.asList(
                    // TODO add SMS if enable
                    EmailAuthCredentialModel.TYPE,
                    WebAuthnCredentialModel.TYPE_TWOFACTOR,
                    OTPCredentialModel.TYPE
            );
            Stream<CredentialModel> credentials = context
                    .getUser()
                    .credentialManager()
                    .getStoredCredentialsStream();
            if (credentials.anyMatch(x -> secondFactors.contains(x.getType()))) {
                    logger.infof("Skip because 2FA is already set");
                    // skip as 2FA is already set
                    return;
            }

            Set<String> availableRequiredActions = Set.of(
                // TODO Append phone is exist
                //PhoneNumberRequiredAction.PROVIDER_ID,
                //PhoneValidationRequiredAction.PROVIDER_ID,
                EmailRequiredAction.PROVIDER_ID,
                UserModel.RequiredAction.CONFIGURE_TOTP.name(),
                WebAuthnRegisterFactory.PROVIDER_ID,
                UserModel.RequiredAction.UPDATE_PASSWORD.name()
            );
            Set<String> authSessionRequiredActions = context.getAuthenticationSession().getRequiredActions();
            authSessionRequiredActions.retainAll(availableRequiredActions);
            if (!authSessionRequiredActions.isEmpty()) {
                    logger.infof("Authentication session required action is not empty no need to force 2FA");
                    // skip as relevant required action is already set
                    return;
            }

            Stream<String> usersRequiredActions = context.getUser().getRequiredActionsStream();
            if (usersRequiredActions.noneMatch(availableRequiredActions::contains)) {
                    logger.infof(
                            "No 2FA method configured for user: %s, setting required action for Email authenticator",
                            context.getUser().getUsername()
                    );
                    context.getUser().addRequiredAction(EmailRequiredAction.PROVIDER_ID);
            }
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel user = context.getUser();

        if (!user.isEmailVerified()) {
            logger.infof("Email not verified for user %s", user.getUsername());
            user.addRequiredAction(RequiredAction.VERIFY_EMAIL.name());
            context.success();
            return;
        }

        Response challenge = context.form()
            .setAttribute("email", user.getEmail())
            .createForm("email-2fa-setup.ftl");
        context.challenge(challenge);
        return;
    }

	@Override
    public void processAction(RequiredActionContext context) {
        UserModel user = context.getUser();
        String email = user.getEmail();

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.success();
            return;
        }

        EmailAuthCredentialProvider smnp = (EmailAuthCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "email_authenticator_credential");

        if (!smnp.isConfiguredFor(context.getRealm(), context.getUser(), EmailAuthCredentialModel.TYPE)) {
            logger.infof("Creating email 2FA credential for user %s with email %s", user.getUsername(), user.getEmail());
            smnp.createCredential(context.getRealm(), context.getUser(), EmailAuthCredentialModel.createEmailAuthenticator(email));
        } else {
            logger.infof("Updating email 2FA credential for user %s with email %s", user.getUsername(), user.getEmail());
            smnp.updateCredential(
                context.getRealm(),
                context.getUser(),
                new UserCredentialModel("random_id", EmailAuthCredentialModel.TYPE, email)
            );
        }

        context.getUser().removeRequiredAction(EmailRequiredAction.PROVIDER_ID);
        context.success();
    }

	@Override
	public void close() {}

	@Override
	public String getCredentialType(KeycloakSession keycloakSession, AuthenticationSessionModel authenticationSessionModel) {
		return EmailAuthCredentialModel.TYPE;
	}
}
