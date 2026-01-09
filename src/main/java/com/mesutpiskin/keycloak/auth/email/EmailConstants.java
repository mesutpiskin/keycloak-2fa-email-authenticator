package com.mesutpiskin.keycloak.auth.email;

/**
 * Constants used for email-based two-factor authentication in Keycloak.
 * <p>
 * This utility class contains all configuration keys and default values
 * for the email authenticator provider.
 * </p>
 *
 * @author Mesut Pişkin
 * @version 26.0.0
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
