# Security Policy

## Supported Versions

Releases are published as `<plugin-version>-KC<keycloak-version>`, one JAR per supported Keycloak release. Security fixes are made on the latest plugin version only.

| Plugin version | Supported |
| :------------- | :-------: |
| Latest release (any `-KC` build) | ✅ |
| Older releases | ❌ |

We recommend always upgrading to the latest release, using the build that matches your Keycloak version.

Vulnerabilities in Keycloak itself should be reported to the [Keycloak project](https://github.com/keycloak/keycloak/security/policy), and vulnerabilities in an email service (SendGrid, AWS SES, Mailgun) to that vendor.

## Reporting a Vulnerability

If you believe you have found a security vulnerability, **please do not open a public GitHub issue**.

Instead, use GitHub's **Private vulnerability reporting** feature by clicking **Security** → **Report a vulnerability** in this repository, or go directly to the [new advisory form](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/security/advisories/new).

When submitting a report, please include:

- A description of the vulnerability
- The affected plugin and Keycloak versions
- The authenticator (Email OTP, Conditional Email OTP, Email OTP (No Enrollment)) and email provider involved, if any
- Steps to reproduce
- Potential impact
- A proof of concept (if applicable)
- Suggested remediation (optional)

Please do not include real credentials, API tokens, or other people's personal data in your report.

## Our Commitment

We will:

- Acknowledge receipt of your report as soon as reasonably possible.
- Investigate the reported issue.
- Keep you informed of our progress.
- Release a fix if the vulnerability is confirmed.
- Credit you for your responsible disclosure, unless you prefer to remain anonymous.

## Scope

Examples of security issues include:

- Bypassing the second factor, for example completing login without a valid OTP or reusing an expired or already used code.
- Weaknesses in OTP generation, such as predictable codes or insufficient randomness.
- Circumventing the configured maximum attempts, resend cooldown, or code TTL, or Keycloak's brute force protection, to enable brute forcing.
- Enrolling the email authenticator for another user, or completing enrollment without proving access to the mailbox.
- Leaking OTP codes, provider credentials (SendGrid, AWS, Mailgun), or users' full email addresses through logs, error messages, the login pages, or the code email.
- Cross-site scripting or injection in the plugin's login templates, code email, or messages.
- Vulnerable dependencies bundled in the released JAR.

The following are generally out of scope:

- Issues that require `simulationMode` to be enabled in production.
- Weaknesses inherent to email as a second factor, such as a compromised mailbox or interception between mail servers.
- Users being auto-enrolled because `autoEnrolIfEmailVerified` is enabled in a realm where `emailVerified` is not trustworthy, as documented.
- Misconfiguration of Keycloak, the realm SMTP settings, the authentication flow, or the email provider account.

General bugs, feature requests, configuration questions, and documentation improvements should be reported through GitHub Issues.

## Disclosure Policy

We follow a coordinated vulnerability disclosure process. Please avoid publicly disclosing security vulnerabilities until a fix has been released or a coordinated disclosure timeline has been agreed upon.
