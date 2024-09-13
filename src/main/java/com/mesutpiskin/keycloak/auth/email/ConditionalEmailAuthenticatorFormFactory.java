package com.mesutpiskin.keycloak.auth.email;

import static java.util.Arrays.asList;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.DEFAULT_OTP_OUTCOME;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_ROLE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OTP_CONTROL_USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_ROLE;
import static org.keycloak.provider.ProviderConfigProperty.LIST_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.ROLE_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

public class ConditionalEmailAuthenticatorFormFactory extends EmailAuthenticatorFormFactory {
	
	public static final String PROVIDER_ID = "email-conditional-authenticator";
    public static final ConditionalEmailAuthenticatorForm SINGLETON = new ConditionalEmailAuthenticatorForm();
	
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Conditional Email OTP";
    }

    @Override
    public String getHelpText() {
        return "Conditional Email otp authenticator.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
    	List<ProviderConfigProperty> list = new ArrayList<>(super.getConfigProperties());
    	
    	 ProviderConfigProperty forceOtpUserAttribute = new ProviderConfigProperty();
         forceOtpUserAttribute.setType(STRING_TYPE);
         forceOtpUserAttribute.setName(OTP_CONTROL_USER_ATTRIBUTE);
         forceOtpUserAttribute.setLabel("OTP control User Attribute");
         forceOtpUserAttribute.setHelpText("The name of the user attribute to explicitly control OTP auth. " +
                 "If attribute value is 'force' then OTP is always required. " +
                 "If value is 'skip' the OTP auth is skipped. Otherwise this check is ignored.");
         list.add(forceOtpUserAttribute);
         
         ProviderConfigProperty skipOtpRole = new ProviderConfigProperty();
         skipOtpRole.setType(ROLE_TYPE);
         skipOtpRole.setName(SKIP_OTP_ROLE);
         skipOtpRole.setLabel("Skip OTP for Role");
         skipOtpRole.setHelpText("OTP is always skipped if user has the given Role.");
         list.add(skipOtpRole);
         
         ProviderConfigProperty forceOtpRole = new ProviderConfigProperty();
         forceOtpRole.setType(ROLE_TYPE);
         forceOtpRole.setName(FORCE_OTP_ROLE);
         forceOtpRole.setLabel("Force OTP for Role");
         forceOtpRole.setHelpText("OTP is always required if user has the given Role.");
         list.add(forceOtpRole);
         
         ProviderConfigProperty skipOtpForHttpHeader = new ProviderConfigProperty();
         skipOtpForHttpHeader.setType(STRING_TYPE);
         skipOtpForHttpHeader.setName(SKIP_OTP_FOR_HTTP_HEADER);
         skipOtpForHttpHeader.setLabel("Skip OTP for Header");
         skipOtpForHttpHeader.setHelpText("OTP is skipped if a HTTP request header does matches the given pattern." +
                 "Can be used to specify trusted networks via: X-Forwarded-Host: (1.2.3.4|1.2.3.5)." +
                 "In this case requests from 1.2.3.4 and 1.2.3.5 come from a trusted source.");
         skipOtpForHttpHeader.setDefaultValue("");
         list.add(skipOtpForHttpHeader);

         ProviderConfigProperty forceOtpForHttpHeader = new ProviderConfigProperty();
         forceOtpForHttpHeader.setType(STRING_TYPE);
         forceOtpForHttpHeader.setName(FORCE_OTP_FOR_HTTP_HEADER);
         forceOtpForHttpHeader.setLabel("Force OTP for Header");
         forceOtpForHttpHeader.setHelpText("OTP required if a HTTP request header matches the given pattern.");
         forceOtpForHttpHeader.setDefaultValue("");
         list.add(forceOtpForHttpHeader);

         ProviderConfigProperty defaultOutcome = new ProviderConfigProperty();
         defaultOutcome.setType(LIST_TYPE);
         defaultOutcome.setName(DEFAULT_OTP_OUTCOME);
         defaultOutcome.setLabel("Fallback OTP handling");
         defaultOutcome.setOptions(asList(SKIP, FORCE));
         defaultOutcome.setHelpText("What to do in case of every check abstains. Defaults to force OTP authentication.");
         list.add(defaultOutcome);

        return Collections.unmodifiableList(list);
    }
    
    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }
}
