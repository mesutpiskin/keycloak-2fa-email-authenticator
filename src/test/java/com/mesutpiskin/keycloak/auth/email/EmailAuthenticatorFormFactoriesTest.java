package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Contract tests for the email OTP authenticator factories.
 */
@DisplayName("Email authenticator factories")
class EmailAuthenticatorFormFactoriesTest {

    private static final List<String> LOCALES = List.of(
            "ar", "az", "da", "de", "en", "es", "fr", "it", "pt", "ro", "ru", "tr", "zh_TW");

    static Stream<AbstractEmailAuthenticatorFormFactory> allFactories() {
        return Stream.of(
                new EmailAuthenticatorFormFactory(),
                new ConditionalEmailAuthenticatorFormFactory(),
                new NoEnrollmentEmailAuthenticatorFormFactory());
    }

    private static Set<String> configKeys(AbstractEmailAuthenticatorFormFactory factory) {
        return factory.getConfigProperties().stream()
                .map(ProviderConfigProperty::getName)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("PROVIDER_IDS lists exactly the registered factories")
    void testProviderIdsMatchFactories() {
        Set<String> ids = allFactories().map(AbstractEmailAuthenticatorFormFactory::getId).collect(Collectors.toSet());
        assertEquals(AbstractEmailAuthenticatorFormFactory.PROVIDER_IDS, ids);
    }

    @Test
    @DisplayName("isEmailAuthenticator recognises only this extension's ids")
    void testIsEmailAuthenticator() {
        assertTrue(AbstractEmailAuthenticatorFormFactory.isEmailAuthenticator(NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID));
        assertFalse(AbstractEmailAuthenticatorFormFactory.isEmailAuthenticator("auth-otp-form"));
        assertFalse(AbstractEmailAuthenticatorFormFactory.isEmailAuthenticator(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allFactories")
    @DisplayName("Never exposes the removed skipSetup option and always exposes the shared OTP settings")
    void testSharedConfiguration(AbstractEmailAuthenticatorFormFactory factory) {
        Set<String> keys = configKeys(factory);
        assertFalse(keys.contains(SkipSetupMigration.LEGACY_SKIP_SETUP));
        assertTrue(keys.containsAll(Set.of(EmailConstants.EMAIL_PROVIDER_TYPE, EmailConstants.CODE_LENGTH,
                EmailConstants.CODE_TTL, EmailConstants.MAX_ATTEMPTS)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allFactories")
    @DisplayName("Reference category is never null (Keycloak calls equals() on it)")
    void testReferenceCategoryNotNull(AbstractEmailAuthenticatorFormFactory factory) {
        assertNotNull(factory.getReferenceCategory());
    }

    @Test
    @DisplayName("Credential-based factories create a credential-based authenticator, offer enrolment and auto-enrol")
    void testCredentialBasedFactories() {
        for (AbstractEmailAuthenticatorFormFactory factory : List.of(
                new EmailAuthenticatorFormFactory(), new ConditionalEmailAuthenticatorFormFactory())) {
            assertInstanceOf(EmailAuthenticatorForm.class, factory.create(mock(KeycloakSession.class)));
            assertEquals(EmailAuthenticatorCredentialModel.TYPE_ID, factory.getReferenceCategory());
            assertTrue(factory.isUserSetupAllowed());
            assertTrue(configKeys(factory).contains(EmailConstants.AUTO_ENROL_IF_EMAIL_VERIFIED));
        }
    }

    @Test
    @DisplayName("No-enrollment factory is decoupled from the credential type and enrolment")
    void testNoEnrollmentFactory() {
        NoEnrollmentEmailAuthenticatorFormFactory factory = new NoEnrollmentEmailAuthenticatorFormFactory();

        assertInstanceOf(NoEnrollmentEmailAuthenticatorForm.class, factory.create(mock(KeycloakSession.class)));
        assertNotEquals(EmailAuthenticatorCredentialModel.TYPE_ID, factory.getReferenceCategory(),
                "Sharing the credential type would make the account console offer a pointless enrolment");
        assertFalse(factory.isUserSetupAllowed());
        assertFalse(configKeys(factory).contains(EmailConstants.AUTO_ENROL_IF_EMAIL_VERIFIED),
                "Auto-enrol configures enrolment, which this authenticator does not have");
    }

    @Test
    @DisplayName("Every locale labels the no-enrollment option in 'Try Another Way'")
    void testMessageBundlesDefineNoEnrollmentLabels() throws IOException {
        String prefix = NoEnrollmentEmailAuthenticatorFormFactory.PROVIDER_ID;
        for (String locale : LOCALES) {
            Properties messages = loadMessages(locale);
            assertNotNull(messages.getProperty(prefix + "-display-name"), locale + " is missing the display name");
            assertNotNull(messages.getProperty(prefix + "-help-text"), locale + " is missing the help text");
        }
    }

    private static Properties loadMessages(String locale) throws IOException {
        String path = "theme-resources/messages/messages_" + locale + ".properties";
        try (InputStream in = EmailAuthenticatorFormFactoriesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Missing bundle " + path);
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
