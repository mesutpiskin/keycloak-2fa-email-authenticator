# Summary
- add a dedicated email OTP credential type with provider/factory registrations and a self-service required action for enrollment
- enhance the email authenticator flow with credential validation, resend cooldown enforcement, and better error handling
- update form templates and localized strings to support the new setup experience and cooldown messaging
- allow app-initiated flows to cancel setup while blocking cancellation for required actions (with graceful fallbacks when newer APIs are unavailable)

# Feature Details
## Email OTP Credential Lifecycle
- introduce `EmailAuthenticatorCredentialModel`, provider, and factory so the email OTP behaves like a first-class credential within Keycloak's SPI
- register provider factories via `META-INF/services` and expose credential metadata (display name, help text, icon, create action) for admin consoles and REST APIs
- add a required-action factory plus Freemarker template guiding users through enrollment; successful setup persists a labeled credential and removes the outstanding action

## Authentication Flow Enhancements
- refactor `EmailAuthenticatorForm` to implement `CredentialValidator`, ensuring users are configured before challenge and scheduling the required action when they're not
- normalize authenticator configuration (code length, TTL, resend cooldown) with defensive parsing, logging, and fallbacks when admin-provided values are invalid or non-positive
- implement resend cooldown tracking using auth-session notes, surfacing localized cooldown messaging and clearing notes when codes expire, resend, or flows reset
- streamline validation paths for expired, missing, and invalid codes while emitting appropriate events and field-level errors that align with Keycloak messaging

## User Experience Improvements
- extend EN/FR/IT/TR message bundles with setup guidance, failure messaging, and cooldown copy to keep localized UX consistent
- update the setup form to render the cancel button only for app-initiated actions, using reflective fallbacks (`isAppInitiatedAction`, `cancelLogin`) to remain compatible across Keycloak versions
- leverage existing form messaging APIs (`FormMessage`, `setError`) so UI feedback sits alongside native Keycloak styles and accessibility patterns

# Highlights
- end-to-end support for storing, enrolling, and managing email OTP as a two-factor option
- friendlier verification flow that prevents rapid resend loops and guides users through failures
- consistent UI copy across English, French, Italian, and Turkish, including the setup form and cooldown warning

# Testing
- `mvn -q package`





