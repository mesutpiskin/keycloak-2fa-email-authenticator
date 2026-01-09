package com.mesutpiskin.keycloak.auth.email;

public class EmailConstants {
	public static final String CODE = "emailCode";
	public static final String CODE_LENGTH = "length";
	public static final String CODE_TTL = "ttl";
	public static final String SIMULATION_MODE = "simulationMode";
	public static final String RESEND_COOLDOWN = "resendCooldown";
	public static final String CODE_RESEND_AVAILABLE_AFTER = "emailCodeResendAvailableAfter";
	public static final int DEFAULT_LENGTH = 6;
	public static final int DEFAULT_TTL = 300;
	public static final boolean DEFAULT_SIMULATION_MODE = false;
	public static final int DEFAULT_RESEND_COOLDOWN = 30;
}
