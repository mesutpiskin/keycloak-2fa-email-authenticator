package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.http.HttpRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConditionalEmailAuthenticatorFormTest {

    private ConditionalEmailAuthenticatorForm authenticator;
    private AuthenticationFlowContext context;
    private HttpRequest httpRequest;
    private HttpHeaders httpHeaders;
    private AuthenticatorConfigModel configModel;
    private Map<String, String> config;

    @BeforeEach
    void setUp() {
        authenticator = new ConditionalEmailAuthenticatorForm();
        context = mock(AuthenticationFlowContext.class);
        httpRequest = mock(HttpRequest.class);
        httpHeaders = mock(HttpHeaders.class);
        configModel = mock(AuthenticatorConfigModel.class);
        config = new HashMap<>();

        when(context.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getHttpHeaders()).thenReturn(httpHeaders);
        when(context.getAuthenticatorConfig()).thenReturn(configModel);
        when(configModel.getConfig()).thenReturn(config);

        // Default config values to avoid NumberFormatException
        config.put(EmailConstants.CODE_LENGTH, "6");
        config.put(EmailConstants.CODE_TTL, "300");
        config.put(EmailConstants.SIMULATION_MODE, "false");

        // Mock User and Realm to avoid NPEs in other checks (though we focus on header check)
        UserModel user = mock(UserModel.class);
        when(context.getUser()).thenReturn(user);
        when(user.getEmail()).thenReturn("test@example.com"); // Email needed
        when(user.getUsername()).thenReturn("testuser");
        when(user.getAttributeStream(anyString())).thenReturn(java.util.stream.Stream.empty());

        RealmModel realm = mock(RealmModel.class);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn("test-realm");
        when(realm.getDisplayName()).thenReturn("Test Realm");

        // Mock AuthenticationSessionModel
        org.keycloak.sessions.AuthenticationSessionModel session = mock(org.keycloak.sessions.AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(session);

        // Mock KeycloakSession and EmailTemplateProvider
        KeycloakSession keycloakSession = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(keycloakSession);

        org.keycloak.email.EmailTemplateProvider emailTemplateProvider = mock(org.keycloak.email.EmailTemplateProvider.class);
        when(keycloakSession.getProvider(org.keycloak.email.EmailTemplateProvider.class)).thenReturn(emailTemplateProvider);

        // Mock Execution
        org.keycloak.models.AuthenticationExecutionModel execution = mock(org.keycloak.models.AuthenticationExecutionModel.class);
        when(context.getExecution()).thenReturn(execution);
        when(execution.getId()).thenReturn("execution-id");

        // Mock Form
        org.keycloak.forms.login.LoginFormsProvider form = mock(org.keycloak.forms.login.LoginFormsProvider.class);
        when(context.form()).thenReturn(form);
        when(form.setExecution(anyString())).thenReturn(form);
        when(form.createForm(anyString())).thenReturn(mock(jakarta.ws.rs.core.Response.class));
    }

    @Test
    void testForceOtpForHttpHeaderMatch() {
        // Setup config to force OTP for specific header
        config.put(ConditionalEmailAuthenticatorForm.FORCE_OTP_FOR_HTTP_HEADER, "X-Custom-Header:.*force.*");

        // Setup headers
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put("X-Custom-Header", Collections.singletonList("please force me"));
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);

        // Since we are mocking context, calling authenticate will call protected/private methods.
        // But authenticate calls super.authenticate (which calls challenge) if it decides to SHOW_OTP.
        // We can verify behavior by spying or checking side effects.
        // If it decides to SHOW_OTP, it calls super.authenticate(context).
        // Since super.authenticate is an instance method of the class we are testing, we can partial mock it or just let it run.
        // super.authenticate calls context.challenge(...).

        // However, this test class inherits from AbstractUsernameFormAuthenticator which we cannot easily mock the super call of the CUT.
        // But we can verify if context.success() was called (SKIP) or not.

        // Actually, let's just test the logic indirectly.
        // If match, it returns SHOW_OTP -> calls showOtpForm -> super.authenticate -> challenge.
        // If we mock super.authenticate to do nothing? No we can't easily.

        // Let's rely on what method is called on context.
        // If SKIP -> context.success()
        // If SHOW -> context.challenge(...) (via super.authenticate)
        // If ABSTAIN -> falls through to showOtpForm -> context.challenge(...)

        // Wait, ABSTAIN also leads to showOtpForm (SHOW_OTP).
        // So effectively FORCE and ABSTAIN behave the same (SHOW OTP), unless there is a default outcome config.

        // To distinguish FORCE from ABSTAIN, we can use default outcome SKIP.
        config.put(ConditionalEmailAuthenticatorForm.DEFAULT_OTP_OUTCOME, "skip");

        // Case 1: Header matches FORCE -> SHOW OTP
        authenticator.authenticate(context);
        // It should NOT call success()
        verify(context, never()).success();
        // It should call challenge (from super.authenticate -> generateAndSendEmailCode -> ... context.challenge)
        // But generateAndSendEmailCode uses session, etc. mocking might be heavy.

        // Maybe we should just test exception handling for now, as that is the specific TODO.
    }

    @Test
    void testInvalidRegexPattern() {
         // Setup config with invalid regex
        config.put(ConditionalEmailAuthenticatorForm.FORCE_OTP_FOR_HTTP_HEADER, "[unclosed bracket");
        config.put(ConditionalEmailAuthenticatorForm.DEFAULT_OTP_OUTCOME, "skip");

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put("Some-Header", Collections.singletonList("value"));
        when(httpHeaders.getRequestHeaders()).thenReturn(headers);

        // New behavior: Should catch exception, log error, and return false (ABSTAIN), thus falling back to DEFAULT (skip)
        // So authenticate() should complete without exception, and call success() because default is skip.

        authenticator.authenticate(context);

        verify(context).success();
    }
}
