package com.mesutpiskin.keycloak.auth.email;

/**
 * Constants used for email-based two-factor authentication in Keycloak.
 * <p>
 * This utility class contains all configuration keys and default values
 * for the email authenticator provider.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.1.1
 * @since 1.0.0
 */
public final class EmailConstants {

	/**
	 * Authentication session note key for storing the generated email code.
	 */
	public static final String CODE = "emailCode";

	/**
	 * Configuration key for the length of the generated code.
	 * Value should be a positive integer.
	 */
	public static final String CODE_LENGTH = "length";

	/**
	 * Configuration key for the time-to-live (TTL) of the code in seconds.
	 * After this period, the code will expire.
	 */
	public static final String CODE_TTL = "ttl";

	/**
	 * Configuration key for enabling simulation mode.
	 * When enabled, codes are logged instead of being sent via email.
	 */
	public static final String SIMULATION_MODE = "simulationMode";

	/**
	 * Configuration key for the cooldown period in seconds between resend requests.
	 * Users must wait this duration before requesting a new code.
	 */
	public static final String RESEND_COOLDOWN = "resendCooldown";

	/**
	 * Authentication session note key for storing the timestamp when resend becomes
	 * available.
	 */
	public static final String CODE_RESEND_AVAILABLE_AFTER = "emailCodeResendAvailableAfter";

	/**
	 * Default code length (number of digits).
	 */
	public static final int DEFAULT_LENGTH = 6;

	/**
	 * Default code TTL in seconds (5 minutes).
	 */
	public static final int DEFAULT_TTL = 300;

	/**
	 * Default simulation mode setting (disabled).
	 */
	public static final boolean DEFAULT_SIMULATION_MODE = false;

	/**
	 * Default resend cooldown in seconds (30 seconds).
	 */
	public static final int DEFAULT_RESEND_COOLDOWN = 30;

	/**
	 * Configuration key for the maximum number of invalid code attempts
	 * before the code is invalidated and the user must request a new one.
	 */
	public static final String MAX_ATTEMPTS = "maxAttempts";

	/**
	 * Default maximum number of invalid code attempts.
	 */
	public static final int DEFAULT_MAX_ATTEMPTS = 5;

	// Email Provider Configuration

	/**
	 * Configuration key for selecting the email provider type.
	 * Valid values: KEYCLOAK, SENDGRID, AWS_SES, MAILGUN
	 */
	public static final String EMAIL_PROVIDER_TYPE = "emailProviderType";

	/**
	 * Configuration key for SendGrid API key.
	 * Required when EMAIL_PROVIDER_TYPE is set to SENDGRID.
	 */
	public static final String SENDGRID_API_KEY = "sendgridApiKey";

	/**
	 * Configuration key for SendGrid sender email address.
	 * Required when EMAIL_PROVIDER_TYPE is set to SENDGRID.
	 */
	public static final String SENDGRID_FROM_EMAIL = "sendgridFromEmail";

	/**
	 * Configuration key for SendGrid sender display name.
	 * Optional, defaults to the from email address.
	 */
	public static final String SENDGRID_FROM_NAME = "sendgridFromName";

	/**
	 * Configuration key for AWS SES region.
	 * Required when EMAIL_PROVIDER_TYPE is set to AWS_SES.
	 */
	public static final String AWS_SES_REGION = "awsSesRegion";

	/**
	 * Configuration key for AWS Access Key ID.
	 * Required when EMAIL_PROVIDER_TYPE is set to AWS_SES.
	 */
	public static final String AWS_ACCESS_KEY_ID = "awsAccessKeyId";

	/**
	 * Configuration key for AWS Secret Access Key.
	 * Required when EMAIL_PROVIDER_TYPE is set to AWS_SES.
	 */
	public static final String AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey";

	/**
	 * Configuration key for AWS SES sender email address.
	 * Required when EMAIL_PROVIDER_TYPE is set to AWS_SES.
	 */
	public static final String AWS_SES_FROM_EMAIL = "awsSesFromEmail";

	/**
	 * Configuration key for AWS SES sender display name.
	 * Optional, defaults to the from email address.
	 */
	public static final String AWS_SES_FROM_NAME = "awsSesFromName";

	/**
	 * Configuration key for enabling fallback to Keycloak SMTP.
	 * When true, if the primary provider fails, the system will
	 * automatically fall back to Keycloak's built-in SMTP.
	 */
	public static final String ENABLE_FALLBACK = "enableFallback";

	/**
	 * Default email provider type (Keycloak SMTP for backward compatibility).
	 */
	public static final String DEFAULT_EMAIL_PROVIDER = "KEYCLOAK";

	/**
	 * Default fallback setting (enabled for reliability).
	 */
	public static final boolean DEFAULT_ENABLE_FALLBACK = true;

	// Mailgun Configuration

	/**
	 * Configuration key for Mailgun API key.
	 * Required when EMAIL_PROVIDER_TYPE is set to MAILGUN.
	 */
	public static final String MAILGUN_API_KEY = "mailgunApiKey";

	/**
	 * Configuration key for Mailgun sending domain (e.g., mg.example.com).
	 * Required when EMAIL_PROVIDER_TYPE is set to MAILGUN.
	 */
	public static final String MAILGUN_DOMAIN = "mailgunDomain";

	/**
	 * Configuration key for Mailgun sender email address.
	 * Required when EMAIL_PROVIDER_TYPE is set to MAILGUN.
	 */
	public static final String MAILGUN_FROM_EMAIL = "mailgunFromEmail";

	/**
	 * Configuration key for Mailgun sender display name.
	 * Optional, defaults to the from email address.
	 */
	public static final String MAILGUN_FROM_NAME = "mailgunFromName";

	/**
	 * Configuration key for Mailgun API region.
	 * Valid values: "US" (default) or "EU".
	 */
	public static final String MAILGUN_REGION = "mailgunRegion";

	/**
	 * Default Mailgun region (US).
	 */
	public static final String DEFAULT_MAILGUN_REGION = "US";


	/**
	 * Configuration key for showing a masked version of the user's email address
	 * on the OTP form.
	 */
	public static final String SHOW_MASKED_EMAIL_ON_OTP_FORM = "showMaskedEmailOnOtpForm";

	/**
	 * Default setting for showing masked email on the OTP form.
	 */
	public static final boolean DEFAULT_SHOW_MASKED_EMAIL_ON_OTP_FORM = false;

	/**
	 * Configuration key controlling whether users with an email address but no
	 * stored email-authenticator credential are reported as configured by
	 * {@link com.mesutpiskin.keycloak.auth.email.EmailAuthenticatorForm#configuredFor}.
	 * <p>
	 * When {@code true}, any user with an email is eligible to receive an OTP
	 * without prior enrolment — useful for admin-provisioned accounts (#112) and
	 * for showing the plugin in Keycloak's "Try Another Way" alternative list
	 * (#50).
	 * </p>
	 * <p>
	 * When {@code false} (the default), only users with a stored credential are
	 * reported as configured, matching Keycloak's convention for built-in
	 * authenticators. This is the setting "Conditional - User Configured"
	 * sub-flows expect, so non-enrolled users are not unexpectedly prompted for
	 * an email OTP (#108 follow-up).
	 * </p>
	 */
	public static final String SKIP_SETUP = "skipSetup";

	/**
	 * Default value for {@link #SKIP_SETUP}. {@code false} is the strict,
	 * convention-aligned default so conditional sub-flows behave as admins
	 * expect; opt in to the permissive behaviour by setting the flag to
	 * {@code true} on the email-authenticator execution.
	 */
	public static final boolean DEFAULT_SKIP_SETUP = false;

	/**
	 * Millisecond rounding offset used for converting milliseconds to seconds.
	 * Adding 999ms before division ensures proper ceiling rounding.
	 */
	public static final long MILLIS_ROUNDING_OFFSET = 999L;

	/**
	 * Private constructor to prevent instantiation of this utility class.
	 *
	 * @throws UnsupportedOperationException if instantiation is attempted
	 */
	private EmailConstants() {
		throw new UnsupportedOperationException("EmailConstants is a utility class and cannot be instantiated");
	}
}
