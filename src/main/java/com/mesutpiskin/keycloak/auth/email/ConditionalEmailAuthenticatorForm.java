package com.mesutpiskin.keycloak.auth.email;

import static com.mesutpiskin.keycloak.auth.email.ConditionalEmailAuthenticatorForm.OtpDecision.ABSTAIN;
import static com.mesutpiskin.keycloak.auth.email.ConditionalEmailAuthenticatorForm.OtpDecision.SHOW_OTP;
import static com.mesutpiskin.keycloak.auth.email.ConditionalEmailAuthenticatorForm.OtpDecision.SKIP_OTP;
import static org.keycloak.models.utils.KeycloakModelUtils.getRoleFromString;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.MultivaluedMap;

public class ConditionalEmailAuthenticatorForm extends EmailAuthenticatorForm {

    public static final String SKIP = "skip";

    public static final String FORCE = "force";

    public static final String OTP_CONTROL_USER_ATTRIBUTE = "otpControlAttribute";

    public static final String SKIP_OTP_ROLE = "skipOtpRole";

    public static final String FORCE_OTP_ROLE = "forceOtpRole";

    public static final String SKIP_OTP_FOR_HTTP_HEADER = "noOtpRequiredForHeaderPattern";

    public static final String FORCE_OTP_FOR_HTTP_HEADER = "forceOtpForHeaderPattern";

    public static final String DEFAULT_OTP_OUTCOME = "defaultOtpOutcome";

    enum OtpDecision {
        SKIP_OTP, SHOW_OTP, ABSTAIN
    }
	
	@Override
    public void authenticate(AuthenticationFlowContext context) {

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        if (tryConcludeBasedOn(voteForUserOtpControlAttribute(context.getUser(), config), context)) {
            return;
        }

        if (tryConcludeBasedOn(voteForUserRole(context.getRealm(), context.getUser(), config), context)) {
            return;
        }

        if (tryConcludeBasedOn(voteForHttpHeaderMatchesPattern(context.getHttpRequest().getHttpHeaders().getRequestHeaders(), config), context)) {
            return;
        }

        if (tryConcludeBasedOn(voteForDefaultFallback(config), context)) {
            return;
        }

        showOtpForm(context);
    }

    private OtpDecision voteForDefaultFallback(Map<String, String> config) {

        if (!config.containsKey(DEFAULT_OTP_OUTCOME)) {
            return ABSTAIN;
        }

        switch (config.get(DEFAULT_OTP_OUTCOME)) {
            case SKIP:
                return SKIP_OTP;
            case FORCE:
                return SHOW_OTP;
            default:
                return ABSTAIN;
        }
    }

    private boolean tryConcludeBasedOn(OtpDecision state, AuthenticationFlowContext context) {

        switch (state) {

            case SHOW_OTP:
                showOtpForm(context);
                return true;

            case SKIP_OTP:
                context.success();
                return true;

            default:
                return false;
        }
    }

    private void showOtpForm(AuthenticationFlowContext context) {
        super.authenticate(context);
    }

    private OtpDecision voteForUserOtpControlAttribute(UserModel user, Map<String, String> config) {

        if (!config.containsKey(OTP_CONTROL_USER_ATTRIBUTE)) {
            return ABSTAIN;
        }

        String attributeName = config.get(OTP_CONTROL_USER_ATTRIBUTE);
        if (attributeName == null) {
            return ABSTAIN;
        }

        Optional<String> value = user.getAttributeStream(attributeName).findFirst();
        if (!value.isPresent()) {
            return ABSTAIN;
        }

        switch (value.get().trim()) {
            case SKIP:
                return SKIP_OTP;
            case FORCE:
                return SHOW_OTP;
            default:
                return ABSTAIN;
        }
    }

    private OtpDecision voteForHttpHeaderMatchesPattern(MultivaluedMap<String, String> requestHeaders, Map<String, String> config) {

        if (!config.containsKey(FORCE_OTP_FOR_HTTP_HEADER) && !config.containsKey(SKIP_OTP_FOR_HTTP_HEADER)) {
            return ABSTAIN;
        }

        //Inverted to allow white-lists, e.g. for specifying trusted remote hosts: X-Forwarded-Host: (1.2.3.4|1.2.3.5)
        if (containsMatchingRequestHeader(requestHeaders, config.get(SKIP_OTP_FOR_HTTP_HEADER))) {
            return SKIP_OTP;
        }

        if (containsMatchingRequestHeader(requestHeaders, config.get(FORCE_OTP_FOR_HTTP_HEADER))) {
            return SHOW_OTP;
        }

        return ABSTAIN;
    }

    private boolean containsMatchingRequestHeader(MultivaluedMap<String, String> requestHeaders, String headerPattern) {

        if (headerPattern == null) {
            return false;
        }

        //TODO cache RequestHeader Patterns
        //TODO how to deal with pattern syntax exceptions?
        // need CASE_INSENSITIVE flag so that we also have matches when the underlying container use a different case than what
        // is usually expected (e.g.: vertx)
        Pattern pattern = Pattern.compile(headerPattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {

            String key = entry.getKey();

            for (String value : entry.getValue()) {

                String headerEntry = key.trim() + ": " + value.trim();

                if (pattern.matcher(headerEntry).matches()) {
                    return true;
                }
            }
        }

        return false;
    }

    private OtpDecision voteForUserRole(RealmModel realm, UserModel user, Map<String, String> config) {

        if (!config.containsKey(SKIP_OTP_ROLE) && !config.containsKey(FORCE_OTP_ROLE)) {
            return ABSTAIN;
        }

        if (userHasRole(realm, user, config.get(SKIP_OTP_ROLE))) {
            return SKIP_OTP;
        }

        if (userHasRole(realm, user, config.get(FORCE_OTP_ROLE))) {
            return SHOW_OTP;
        }

        return ABSTAIN;
    }

    private boolean userHasRole(RealmModel realm, UserModel user, String roleName) {

        if (roleName == null) {
            return false;
        }

        RoleModel role = getRoleFromString(realm, roleName);
        if (role != null) {
            return user.hasRole(role);
        }
        return false;
    }
}
