package com.mesutpiskin.keycloak.auth.email;

import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.services.managers.BruteForceProtector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Reports a wrong email code to Keycloak's brute force protector.
 * <p>
 * From 26.6 on, Keycloak only counts failures whose authentication category
 * is {@code password}, {@code otp} or {@code recovery-authn-codes}. The failure
 * raised through {@code failureChallenge} carries this extension's own
 * reference category, so Keycloak drops it and the realm lockout never applies
 * to a wrong email code. The email code is a one-time password, so the failure
 * is reported here under {@code otp}.
 * </p>
 * <p>
 * {@code BruteForceProtector.failedLogin} is private SPI and its signature has
 * changed between minor releases:
 * </p>
 * <ul>
 * <li>26.7 and later: {@code (realm, user, connection, uriInfo, Set<String>)}</li>
 * <li>26.6: {@code (realm, user, connection, uriInfo, String)}</li>
 * <li>26.5 and earlier: {@code (realm, user, connection, uriInfo)}, with no
 * category</li>
 * </ul>
 * <p>
 * A direct call links against one of them only and fails with
 * {@code NoSuchMethodError} on the others, which would turn every wrong code
 * into a server error. The method is therefore resolved once, reflectively,
 * when this class is first used.
 * </p>
 * <p>
 * When neither category-aware signature exists (26.5 and earlier) the call is
 * skipped on purpose. Those versions have no category filter, so the failure
 * raised through {@code failureChallenge} already counts towards the lockout;
 * reporting it again here would count every wrong code twice. The login is
 * never failed because of this report.
 * </p>
 * <p>
 * Under {@code otp}, Keycloak also counts the failure towards the realm's
 * secondary-factor limit ({@code maxSecondaryAuthFailures}), which is cleared
 * only by a login that includes an {@code otp} credential. The form therefore
 * accepts a correct code with {@code context.success(CATEGORY)}.
 * </p>
 */
final class BruteForceFailureReporter {

    static final String CATEGORY = OTPCredentialModel.TYPE;

    private static final Logger logger = Logger.getLogger(BruteForceFailureReporter.class);

    private static final Invoker INVOKER = resolve(BruteForceProtector.class);

    private BruteForceFailureReporter() {
    }

    /** Calls the resolved {@code failedLogin} variant with the {@code otp} category. */
    @FunctionalInterface
    interface Invoker {
        void failedLogin(Object protector, RealmModel realm, UserModel user, ClientConnection connection,
                UriInfo uriInfo) throws ReflectiveOperationException;
    }

    /**
     * Counts one failed attempt for {@code user} towards the realm lockout. Never
     * throws: a problem here is logged and the login flow continues.
     */
    static void report(AuthenticationFlowContext context, UserModel user) {
        report(INVOKER, context, user);
    }

    static void report(Invoker invoker, AuthenticationFlowContext context, UserModel user) {
        if (invoker == null) {
            return;
        }
        try {
            BruteForceProtector protector = context.getSession().getProvider(BruteForceProtector.class);
            if (protector == null) {
                return;
            }
            invoker.failedLogin(protector, context.getRealm(), user, context.getConnection(), context.getUriInfo());
        } catch (InvocationTargetException e) {
            logger.warnf(e.getCause(), "Could not report a wrong email code to the brute force protector");
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.warnf(e, "Could not report a wrong email code to the brute force protector");
        }
    }

    /**
     * Finds the category-aware {@code failedLogin} on {@code protectorType}:
     * the {@code Set<String>} variant first, then the {@code String} one.
     *
     * @return an invoker for the variant found, or {@code null} when this
     *         Keycloak version has neither
     */
    static Invoker resolve(Class<?> protectorType) {
        Method withSet = find(protectorType, Set.class);
        if (withSet != null) {
            return (protector, realm, user, connection, uriInfo) -> withSet.invoke(protector, realm, user,
                    connection, uriInfo, Set.of(CATEGORY));
        }
        Method withString = find(protectorType, String.class);
        if (withString != null) {
            return (protector, realm, user, connection, uriInfo) -> withString.invoke(protector, realm, user,
                    connection, uriInfo, CATEGORY);
        }
        logger.debugf("This Keycloak version has no BruteForceProtector.failedLogin that accepts an authentication "
                + "category, so it does not filter failures by category: wrong email codes already count towards "
                + "the realm's brute force lockout and are not reported a second time.");
        return null;
    }

    private static Method find(Class<?> protectorType, Class<?> categoryType) {
        try {
            return protectorType.getMethod("failedLogin", RealmModel.class, UserModel.class, ClientConnection.class,
                    UriInfo.class, categoryType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
