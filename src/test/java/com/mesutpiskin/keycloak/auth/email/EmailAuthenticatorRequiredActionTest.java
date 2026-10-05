package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for how {@link EmailAuthenticatorRequiredAction} picks the Email OTP
 * configuration used during enrolment.
 */
@DisplayName("EmailAuthenticatorRequiredAction config lookup")
class EmailAuthenticatorRequiredActionTest {

    private final EmailAuthenticatorRequiredAction action = new EmailAuthenticatorRequiredAction();
    private final List<AuthenticationExecutionModel> executions = new ArrayList<>();
    private final Map<String, AuthenticatorConfigModel> configs = new HashMap<>();
    private RequiredActionContext context;

    @BeforeEach
    void setUp() {
        RealmModel realm = mock(RealmModel.class);
        context = mock(RequiredActionContext.class);
        when(context.getRealm()).thenReturn(realm);

        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setId("flow-1");
        when(realm.getAuthenticationFlowsStream()).thenAnswer(inv -> Stream.of(flow));
        when(realm.getAuthenticationExecutionsStream("flow-1")).thenAnswer(inv -> executions.stream());
        when(realm.getAuthenticatorConfigById(any())).thenAnswer(inv -> configs.get(inv.<String>getArgument(0)));
    }

    private void execution(String authenticator, String configId, Map<String, String> config) {
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setAuthenticator(authenticator);
        execution.setAuthenticatorConfig(configId);
        executions.add(execution);
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(configId);
        model.setConfig(config);
        configs.put(configId, model);
    }

    @Test
    @DisplayName("Prefers an enrolment-capable execution even when a no-enrollment one comes first")
    void testPrefersEnrolmentCapableExecution() {
        execution(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(EmailConstants.EMAIL_PROVIDER_TYPE, "MAILGUN"));
        execution(EmailAuthenticatorFormFactory.PROVIDER_ID, "c2",
                Map.of(EmailConstants.AUTO_ENROL_IF_EMAIL_VERIFIED, "true"));

        assertEquals("true", action.findAuthenticatorConfig(context).get(EmailConstants.AUTO_ENROL_IF_EMAIL_VERIFIED),
                "Only the enrolment-capable execution carries the auto-enrol setting");
    }

    @Test
    @DisplayName("Falls back to a no-enrollment execution when the realm has no other, so its email provider still applies")
    void testFallsBackToNoEnrollmentExecution() {
        execution(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(EmailConstants.EMAIL_PROVIDER_TYPE, "MAILGUN"));

        assertEquals("MAILGUN", action.findAuthenticatorConfig(context).get(EmailConstants.EMAIL_PROVIDER_TYPE));
    }

    @Test
    @DisplayName("Ignores executions of other authenticators")
    void testIgnoresForeignAuthenticators() {
        execution("auth-otp-form", "c1", Map.of(EmailConstants.EMAIL_PROVIDER_TYPE, "MAILGUN"));

        assertTrue(action.findAuthenticatorConfig(context).isEmpty());
    }
}
