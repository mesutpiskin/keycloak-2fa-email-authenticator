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
 * @author <a href="mailto:alistair.doswald@elca.ch">Alistair Doswald</a>
 * @author Netzbegruenung e.V.
 * @author verdigado eG
 * @author <a href="mailto:christophe@kyvrakidis.com">Christophe Kyvrakidis</a>
 */

package com.mesutpiskin.keycloak.auth.email;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class EmailAuthCredentialModel extends CredentialModel {
	public static final String TYPE = "email-authenticator";

	private final EmailAuthCredentialData emailAuthenticatorData;


	private EmailAuthCredentialModel(EmailAuthCredentialData emailAuthenticator) {
		this.emailAuthenticatorData = emailAuthenticator;
	}

	private EmailAuthCredentialModel(String emailAuthenticatorString) {
		emailAuthenticatorData = new EmailAuthCredentialData(emailAuthenticatorString);
	}

	public static EmailAuthCredentialModel createFromModel(CredentialModel credentialModel){
		try {
			EmailAuthCredentialData credentialData = JsonSerialization.readValue(credentialModel.getCredentialData(), EmailAuthCredentialData.class);

			EmailAuthCredentialModel emailAuthenticatorModel = new EmailAuthCredentialModel(credentialData);
			emailAuthenticatorModel.setUserLabel(
					"Email : ***" + credentialData.getEmailAuthenticator().substring(
							Math.max(credentialData.getEmailAuthenticator().length() - 3, 0)
					)
			);
			emailAuthenticatorModel.setCreatedDate(credentialModel.getCreatedDate());
			emailAuthenticatorModel.setType(TYPE);
			emailAuthenticatorModel.setId(credentialModel.getId());
			emailAuthenticatorModel.setCredentialData(credentialModel.getCredentialData());
			return emailAuthenticatorModel;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static EmailAuthCredentialModel createEmailAuthenticator(String emailAuthenticator) {
		EmailAuthCredentialModel credentialModel = new EmailAuthCredentialModel(emailAuthenticator);
		credentialModel.fillCredentialModelFields();
		return credentialModel;
	}

	public EmailAuthCredentialData getEmailAuthenticatorData() {
		return emailAuthenticatorData;
	}

	private void fillCredentialModelFields(){
		try {
			setCredentialData(JsonSerialization.writeValueAsString(emailAuthenticatorData));
			setType(TYPE);
			setCreatedDate(Time.currentTimeMillis());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
