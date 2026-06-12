# Keycloak 2FA Email Authenticator

A Keycloak Authentication Provider for two-factor authentication (2FA) via email OTP. Supports Keycloak SMTP, SendGrid, AWS SES, and Mailgun.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mesutpiskin/keycloak-2fa-email-authenticator.svg)](https://central.sonatype.com/artifact/io.github.mesutpiskin/keycloak-2fa-email-authenticator)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

## Documentation

Full documentation — installation, configuration, template customization, and contribution guide — is available at:

**https://mesutpiskin.github.io/keycloak-2fa-email-authenticator/**

## Highlights

- Email OTP login for Keycloak browser flows
- Configurable code length, TTL, resend cooldown, and max attempts
- Optional **masked email display** on the OTP form for better UX after the code is sent
- Multiple email delivery backends: Keycloak SMTP, SendGrid, AWS SES, and Mailgun

## Related projects

Need OTP via SMS, Telegram, WhatsApp, or Signal? See the sibling project: **[keycloak-2fa-messaging-authenticator](https://github.com/mesutpiskin/keycloak-2fa-messaging-authenticator)** — same approach, different channel.

## Quick Start

The easiest way to get the JAR is via Maven Central. Use the version matching your Keycloak installation:

**Maven:**
```xml
<dependency>
  <groupId>io.github.mesutpiskin</groupId>
  <artifactId>keycloak-2fa-email-authenticator</artifactId>
  <version>26.3.0-KC26.6.1</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'io.github.mesutpiskin:keycloak-2fa-email-authenticator:26.3.0-KC26.6.1'
```

> Version format: `<plugin-version>-KC<keycloak-version>` — all versions on [Maven Central](https://central.sonatype.com/artifact/io.github.mesutpiskin/keycloak-2fa-email-authenticator).

## Contributing

Contributions are welcome — bug reports, feature requests, translations, and pull requests. Please open an issue first for significant changes.

## Sponsor

This project is developed and maintained voluntarily.
If you'd like to support it, donations go to [KACUV](https://kacuv.org/en/) —
a non-profit supporting children in need.

[![Donate](https://img.shields.io/badge/Donate-KACUV-blue)](https://kacuv.org/en/)

## License

Licensed under the [Apache License 2.0](LICENSE).
