package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Unit tests for {@link SkipSetupMigration}.
 */
@DisplayName("SkipSetupMigration Tests")
class SkipSetupMigrationTest {

    private RealmModel realm;
    private List<AuthenticationExecutionModel> executions;
    private Map<String, AuthenticatorConfigModel> configs;

    @BeforeEach
    void setUp() {
        realm = mock(RealmModel.class);
        executions = new ArrayList<>();
        configs = new HashMap<>();

        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setId("flow-1");
        flow.setAlias("browser-email");
        when(realm.getName()).thenReturn("test");
        when(realm.getAuthenticationFlowsStream()).thenAnswer(inv -> Stream.of(flow));
        when(realm.getAuthenticationExecutionsStream("flow-1")).thenAnswer(inv -> executions.stream());
        when(realm.getAuthenticatorConfigById(any())).thenAnswer(inv -> configs.get(inv.<String>getArgument(0)));
    }

    private AuthenticationExecutionModel execution(String id, String authenticator, String configId,
            Map<String, String> config) {
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setId(id);
        execution.setAuthenticator(authenticator);
        execution.setAuthenticatorConfig(configId);
        executions.add(execution);
        if (configId != null && !configs.containsKey(configId)) {
            AuthenticatorConfigModel model = new AuthenticatorConfigModel();
            model.setId(configId);
            model.setConfig(new HashMap<>(config));
            configs.put(configId, model);
        }
        return execution;
    }

    @Test
    @DisplayName("Switches email-authenticator with skipSetup=true to the no-enrollment authenticator and keeps its other settings")
    void testMigratesSkipSetupTrue() {
        AuthenticationExecutionModel exec = execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true", EmailConstants.CODE_LENGTH, "8"));

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, exec.getAuthenticator());
        assertEquals("c1", exec.getAuthenticatorConfig(), "Configuration must stay attached");
        verify(realm).updateAuthenticatorExecution(exec);
        assertEquals(Map.of(EmailConstants.CODE_LENGTH, "8"), configs.get("c1").getConfig());
        verify(realm).updateAuthenticatorConfig(configs.get("c1"));
    }

    @Test
    @DisplayName("Accepts a padded / mixed-case 'true'")
    void testMigratesLenientTrue() {
        AuthenticationExecutionModel exec = execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, " TRUE "));

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, exec.getAuthenticator());
    }

    @Test
    @DisplayName("skipSetup=false keeps the authenticator but removes the obsolete key")
    void testSkipSetupFalse_onlyCleansKey() {
        AuthenticationExecutionModel exec = execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "false"));

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(EmailAuthenticatorFormFactory.PROVIDER_ID, exec.getAuthenticator());
        verify(realm, never()).updateAuthenticatorExecution(any());
        assertTrue(configs.get("c1").getConfig().isEmpty());
        verify(realm).updateAuthenticatorConfig(configs.get("c1"));
    }

    @Test
    @DisplayName("Conditional Email OTP cannot be migrated: keeps its authenticator, key is removed (warning only)")
    void testConditional_notMigrated() {
        AuthenticationExecutionModel exec = execution("e1", ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID, exec.getAuthenticator());
        verify(realm, never()).updateAuthenticatorExecution(any());
        assertFalse(configs.get("c1").getConfig().containsKey(SkipSetupMigration.LEGACY_SKIP_SETUP));
    }

    @Test
    @DisplayName("Ignores executions of other authenticators, even with a skipSetup key")
    void testIgnoresForeignAuthenticators() {
        execution("e1", "auth-otp-form", "c1", Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));

        SkipSetupMigration.migrateRealm(realm);

        verify(realm, never()).updateAuthenticatorExecution(any());
        verify(realm, never()).updateAuthenticatorConfig(any());
        assertTrue(configs.get("c1").getConfig().containsKey(SkipSetupMigration.LEGACY_SKIP_SETUP));
    }

    @Test
    @DisplayName("Does nothing for executions without config or without the key")
    void testNoConfigOrNoKey_noWrites() {
        execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, null, Map.of());
        execution("e2", EmailAuthenticatorFormFactory.PROVIDER_ID, "c2", Map.of(EmailConstants.CODE_LENGTH, "6"));

        SkipSetupMigration.migrateRealm(realm);

        verify(realm, never()).updateAuthenticatorExecution(any());
        verify(realm, never()).updateAuthenticatorConfig(any());
    }

    @Test
    @DisplayName("A config shared by several executions migrates each of them before the key is removed")
    void testSharedConfig_migratesAllExecutions() {
        Map<String, String> shared = Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true");
        AuthenticationExecutionModel first = execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1", shared);
        AuthenticationExecutionModel second = execution("e2", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1", shared);

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, first.getAuthenticator());
        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, second.getAuthenticator());
        verify(realm, times(1)).updateAuthenticatorConfig(configs.get("c1"));
    }

    @Test
    @DisplayName("Is idempotent: a second run finds nothing to do")
    void testIdempotent() {
        execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));

        SkipSetupMigration.migrateRealm(realm);
        clearInvocations(realm);
        SkipSetupMigration.migrateRealm(realm);

        verify(realm, never()).updateAuthenticatorExecution(any());
        verify(realm, never()).updateAuthenticatorConfig(any());
    }

    @Test
    @DisplayName("Copes with an unmodifiable config map from the storage layer")
    void testUnmodifiableConfigMap() {
        execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1", Map.of());
        configs.get("c1").setConfig(Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));

        assertDoesNotThrow(() -> SkipSetupMigration.migrateRealm(realm));
        assertTrue(configs.get("c1").getConfig().isEmpty());
    }

    @Test
    @DisplayName("Realm-wide: skipSetup=true on one Email OTP execution migrates every Email OTP execution in the realm")
    void testRealmWide_migratesExecutionsWithoutTheKey() {
        AuthenticationExecutionModel flagged = execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));
        AuthenticationExecutionModel unflagged = execution("e2", EmailAuthenticatorFormFactory.PROVIDER_ID, "c2",
                Map.of(EmailConstants.CODE_LENGTH, "6"));
        AuthenticationExecutionModel noConfig = execution("e3", EmailAuthenticatorFormFactory.PROVIDER_ID, null, Map.of());

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, flagged.getAuthenticator());
        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, unflagged.getAuthenticator(),
                "The old flag applied to the whole realm, so this execution was permissive too");
        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, noConfig.getAuthenticator());
        verify(realm, never()).updateAuthenticatorConfig(configs.get("c2"));
    }

    @Test
    @DisplayName("Realm-wide: skipSetup=true on a Conditional Email OTP execution still migrates the realm's Email OTP executions")
    void testRealmWide_conditionalOptInMigratesEmailOtp() {
        AuthenticationExecutionModel conditional = execution("e1", ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID,
                "c1", Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));
        AuthenticationExecutionModel email = execution("e2", EmailAuthenticatorFormFactory.PROVIDER_ID, null, Map.of());

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, email.getAuthenticator());
        assertEquals(ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID, conditional.getAuthenticator());
    }

    @Test
    @DisplayName("A skipSetup key on a no-enrollment execution does not opt the realm in (the old lookup never read it)")
    void testNoEnrollmentKey_doesNotOptIn() {
        execution("e1", NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID, "c1",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "true"));
        AuthenticationExecutionModel email = execution("e2", EmailAuthenticatorFormFactory.PROVIDER_ID, null, Map.of());

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(EmailAuthenticatorFormFactory.PROVIDER_ID, email.getAuthenticator());
        assertTrue(configs.get("c1").getConfig().isEmpty(), "The stale key is still removed");
    }

    @Test
    @DisplayName("A realm without skipSetup=true anywhere is left as it is")
    void testNoOptIn_realmUntouched() {
        AuthenticationExecutionModel email = execution("e1", EmailAuthenticatorFormFactory.PROVIDER_ID, null, Map.of());
        execution("e2", ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID, "c2",
                Map.of(SkipSetupMigration.LEGACY_SKIP_SETUP, "false"));

        SkipSetupMigration.migrateRealm(realm);

        assertEquals(EmailAuthenticatorFormFactory.PROVIDER_ID, email.getAuthenticator());
        verify(realm, never()).updateAuthenticatorExecution(any());
    }

    @Test
    @DisplayName("A failure in one realm is logged and does not stop the other realms")
    void testMigrateEach_isolatesFailures() {
        List<String> migrated = new ArrayList<>();

        assertDoesNotThrow(() -> SkipSetupMigration.migrateEach(List.of("r1", "r2", "r3"), realmId -> {
            if ("r2".equals(realmId)) {
                throw new IllegalStateException("broken realm");
            }
            migrated.add(realmId);
        }));

        assertEquals(List.of("r1", "r3"), migrated);
    }
}
