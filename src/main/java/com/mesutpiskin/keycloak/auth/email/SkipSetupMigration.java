package com.mesutpiskin.keycloak.auth.email;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * One-shot startup migration for the removed {@code skipSetup} option
 * ("Treat any user with an email as configured").
 * <p>
 * Before 26.7.0 the option was <strong>realm-wide</strong>: a single
 * {@code skipSetup=true} on any Email OTP or Conditional Email OTP execution
 * made <em>every</em> such execution in the realm treat all users with an email
 * as configured. That option has been replaced by a separate authenticator,
 * {@link NoEnrollmentEmailAuthenticatorForm}, because a flag cannot change
 * whether Keycloak treats an authenticator as credential-based (issue #147).
 * </p>
 * <p>
 * Silently dropping the flag would weaken security. Keycloak treats an
 * unconfigured REQUIRED step as completed, so users who never enrolled would
 * log in without an OTP. To prevent that, this migration runs on every startup,
 * after Keycloak's own migrations. For each realm where the flag was in effect,
 * it:
 * </p>
 * <ol>
 * <li>Switches <em>every</em> {@code email-authenticator} execution in the realm
 * to {@value NoEnrollmentEmailAuthenticatorFormFactory#PROVIDER_ID}, including
 * executions that did not carry the flag themselves. Each execution keeps its
 * configuration (provider, OTP policy).</li>
 * <li>Logs a WARN for every Conditional Email OTP execution in the realm. There
 * is no no-enrollment conditional variant, so those executions now apply only to
 * enrolled users, and the admin has to decide what to do.</li>
 * </ol>
 * <p>
 * In every realm, it then removes the obsolete {@code skipSetup} key from every
 * Email OTP configuration, so the next startup finds nothing and the migration
 * does nothing.
 * </p>
 * <p>
 * One intended difference remains: a REQUIRED no-enrollment execution blocks
 * users who have no email address ("credential setup required"), where the old
 * flag let them pass the step without an OTP.
 * </p>
 * <p>
 * Each realm is migrated in its own transaction, so a failure in one realm is
 * logged and does not block the others or Keycloak's startup. Realms imported
 * after startup (for example a partial import of an old export) are migrated on
 * the next restart.
 * </p>
 * <p>
 * This class is temporary upgrade code, not permanent architecture. Remove it,
 * together with its registration in
 * {@link NoEnrollmentEmailAuthenticatorFormFactory#postInit}, once supported
 * upgrade paths no longer start from a release that had {@code skipSetup}.
 * </p>
 *
 * @since 26.7.0
 */
public final class SkipSetupMigration {

    /**
     * Configuration key of the removed option. Only this migration should ever
     * read it; it is package-private so tests can use it.
     */
    static final String LEGACY_SKIP_SETUP = "skipSetup";

    private static final Logger logger = Logger.getLogger(SkipSetupMigration.class);

    private SkipSetupMigration() {
    }

    /**
     * Migrates every realm, each one in its own transaction. Intended to run
     * after Keycloak's own migrations. A realm that fails is logged and left for
     * the next startup, and the remaining realms are still migrated.
     *
     * @param factory the session factory used to open the transactions
     */
    public static void migrateAllRealms(KeycloakSessionFactory factory) {
        List<String> realmIds = KeycloakModelUtils.runJobInTransactionWithResult(factory,
                session -> session.realms().getRealmsStream().map(RealmModel::getId).toList());

        migrateEach(realmIds, realmId -> KeycloakModelUtils.runJobInTransaction(factory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm != null) {
                migrateRealm(realm);
            }
        }));
    }

    /**
     * Runs {@code migrateOne} for every realm id, isolating failures so one
     * broken realm cannot stop the others. Package-private for tests.
     */
    static void migrateEach(List<String> realmIds, Consumer<String> migrateOne) {
        for (String realmId : realmIds) {
            try {
                migrateOne.accept(realmId);
            } catch (RuntimeException e) {
                logger.errorf(e, "Could not migrate the removed 'skipSetup' option in realm %s; "
                        + "it will be retried on the next startup.", realmId);
            }
        }
    }

    /**
     * Migrates one realm. Running it again on an already migrated realm does
     * nothing.
     *
     * @param realm the realm to migrate
     */
    static void migrateRealm(RealmModel realm) {
        // Materialise first: the loop updates the executions it iterates over.
        List<FlowExecution> executions = realm.getAuthenticationFlowsStream()
                .flatMap(flow -> realm.getAuthenticationExecutionsStream(flow.getId())
                        .filter(execution -> AbstractEmailAuthenticatorFormFactory
                                .isEmailAuthenticator(execution.getAuthenticator()))
                        .map(execution -> new FlowExecution(flow, execution)))
                .toList();

        // Same rule the old realm-wide lookup applied: one explicit 'true' on an
        // Email OTP or Conditional Email OTP execution enabled it for the realm.
        FlowExecution optedIn = executions.stream()
                .filter(flowExecution -> AbstractEmailAuthenticatorFormFactory
                        .isEnrolmentCapable(flowExecution.execution().getAuthenticator()))
                .filter(flowExecution -> isEnabled(legacyValue(realm, flowExecution.execution())))
                .findFirst()
                .orElse(null);

        if (optedIn != null) {
            logger.warnf("Realm '%s' had 'skipSetup=true' (on execution %s in flow '%s'). That option was "
                    + "removed in 26.7.0 and applied to every Email OTP execution in the realm; migrating them.",
                    realm.getName(), optedIn.execution().getId(), optedIn.flow().getAlias());
            executions.forEach(flowExecution -> migrateExecution(realm, flowExecution));
        }

        executions.stream()
                .map(flowExecution -> configOf(realm, flowExecution.execution()))
                .filter(Objects::nonNull)
                .filter(config -> config.getConfig().containsKey(LEGACY_SKIP_SETUP))
                .map(AuthenticatorConfigModel::getId)
                .distinct() // one config may be shared by several executions
                .forEach(configId -> removeLegacyKey(realm, configId));
    }

    private static void migrateExecution(RealmModel realm, FlowExecution flowExecution) {
        AuthenticationExecutionModel execution = flowExecution.execution();
        String authenticator = execution.getAuthenticator();

        if (EmailAuthenticatorFormFactory.PROVIDER_ID.equals(authenticator)) {
            execution.setAuthenticator(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID);
            realm.updateAuthenticatorExecution(execution);
            logger.warnf("Migrated execution %s in flow '%s' of realm '%s' from '%s' to '%s'. Users who have "
                    + "never enrolled still get the OTP; users without an email address are now blocked "
                    + "by this step instead of skipping it.",
                    execution.getId(), flowExecution.flow().getAlias(), realm.getName(),
                    EmailAuthenticatorFormFactory.PROVIDER_ID, NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID);
        } else if (ConditionalEmailAuthenticatorFormFactory.PROVIDER_ID.equals(authenticator)) {
            logger.warnf("Execution %s ('%s') in flow '%s' of realm '%s' was covered by the removed "
                    + "'skipSetup=true' and cannot be migrated automatically: users who have not enrolled the "
                    + "email authenticator are no longer asked for an email OTP by this step. Ask those users "
                    + "to enrol, or redesign the flow around 'Email OTP (No Enrollment)'.",
                    execution.getId(), authenticator, flowExecution.flow().getAlias(), realm.getName());
        }
        // A no-enrollment execution already has the permissive behaviour.
    }

    private static void removeLegacyKey(RealmModel realm, String configId) {
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(configId);
        if (config == null || config.getConfig() == null) {
            return;
        }
        // getConfig() may be unmodifiable depending on the storage layer.
        Map<String, String> cleaned = new HashMap<>(config.getConfig());
        cleaned.remove(LEGACY_SKIP_SETUP);
        config.setConfig(cleaned);
        realm.updateAuthenticatorConfig(config);
        logger.debugf("Removed obsolete '%s' key from authenticator config %s in realm '%s'",
                LEGACY_SKIP_SETUP, configId, realm.getName());
    }

    private static String legacyValue(RealmModel realm, AuthenticationExecutionModel execution) {
        AuthenticatorConfigModel config = configOf(realm, execution);
        return config == null ? null : config.getConfig().get(LEGACY_SKIP_SETUP);
    }

    private static AuthenticatorConfigModel configOf(RealmModel realm, AuthenticationExecutionModel execution) {
        String configId = execution.getAuthenticatorConfig();
        if (configId == null) {
            return null;
        }
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(configId);
        return config == null || config.getConfig() == null ? null : config;
    }

    private static boolean isEnabled(String value) {
        return value != null && Boolean.parseBoolean(value.trim());
    }

    private record FlowExecution(AuthenticationFlowModel flow, AuthenticationExecutionModel execution) {
    }
}
