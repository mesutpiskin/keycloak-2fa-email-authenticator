# 🔒 Keycloak 2FA Email Authenticator

A professional Keycloak Authentication Provider implementation for two-factor authentication (2FA) using One-Time Passwords (OTP) delivered via email through SMTP.

[![Keycloak Version](https://img.shields.io/badge/Keycloak-26.5.0-blue.svg)](https://www.keycloak.org/)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Maven Version](https://img.shields.io/badge/Maven-3.9-green.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Deployment](#-deployment)
  - [Local Build & Deployment](#-local-build--deployment)
  - [Docker/Podman Deployment](#-dockerpodman-deployment)
- [Configuration](#-configuration)
  - [Email Configuration](#email-configuration)
  - [Authentication Flow Setup](#authentication-flow-setup)
- [Local Testing](#-local-testing)
- [Development](#-development)
- [Resources](#-resources)
- [Localization](#-localization)
  - [Supported Languages](#supported-languages)
  - [How to Add a New Language](#how-to-add-a-new-language)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

This Keycloak extension enables email-based two-factor authentication by sending a verification code (OTP) to the user's registered email address during login. The authenticator integrates seamlessly with Keycloak's authentication flow system.

**Key Capabilities:**
- Send OTP codes via multiple email providers (Keycloak SMTP, SendGrid, AWS SES, Mailgun)
- Flexible provider selection with automatic fallback support
- Customizable email templates
- Conditional authentication support
- Integration with Keycloak's authentication flow builder

---

## ✨ Features

- ✅ Email-based OTP authentication
- ✅ **Multiple email provider support** (Keycloak SMTP, SendGrid, AWS SES)
- ✅ **Automatic fallback** to Keycloak SMTP if primary provider fails
- ✅ **SendGrid integration** with API key authentication
- ✅ **AWS SES integration** with IAM credentials and regional support
- ✅ Customizable email templates
- ✅ Conditional authentication flows
- ✅ Multi-stage Docker build support
- ✅ Compatible with Keycloak 26.x
- ✅ Easy integration with existing authentication flows

\* *Mailgun support coming soon*

---

## 📦 Prerequisites

### For Local Build:
- **Java 21** or higher ([Download](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))

### For Docker/Podman Build:
- **Docker** ([Download](https://www.docker.com/)) or **Podman** ([Download](https://podman.io/))
- (Optional) Docker Compose or Podman Compose

---

## 🚀 Deployment

### 🖥 Local Build & Deployment

#### 1. Clone the Repository

```bash
git clone https://github.com/mesutpiskin/keycloak-2fa-email-authenticator.git
cd keycloak-2fa-email-authenticator
```

#### 2. Verify Prerequisites

Check your Java and Maven versions:

```bash
java -version  # Should show Java 21+
mvn -version   # Should show Maven 3.9+
```

#### 3. Build the Project

```bash
mvn clean package
```

This creates `target/keycloak-2fa-email-authenticator-v26.0.0-SNAPSHOT.jar`.

> **Note:** If tests fail due to Java version compatibility, use:
> ```bash
> mvn clean package -DskipTests
> ```

#### 4. Deploy to Keycloak

**For Standard Keycloak Installation:**

```bash
cp target/keycloak-2fa-email-authenticator-*.jar <KEYCLOAK_HOME>/providers/
```

**For Dockerized Keycloak:**

```bash
cp target/keycloak-2fa-email-authenticator-*.jar /opt/keycloak/providers/
```

#### 5. Rebuild Keycloak

```bash
<KEYCLOAK_HOME>/bin/kc.sh build
```

---

### 🐳 Docker/Podman Deployment

This project includes a **multi-stage Dockerfile** for streamlined containerized deployment.

#### Build the Container Image

**Using Docker:**

```bash
docker build -t keycloak-2fa-email:latest .
```

**Using Podman:**

```bash
podman build --tls-verify=false -t keycloak-2fa-email:latest .
```

> **Note:** The `--tls-verify=false` flag may be needed with Podman if you encounter certificate verification issues.

#### Run the Container

**Using Docker:**

```bash
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-2fa-email:latest \
  start-dev
```

**Using Podman:**

```bash
podman run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-2fa-email:latest \
  start-dev
```

#### Using Docker Compose

The project includes a `docker-compose.yml` file for simplified deployment:

```bash
docker-compose up
```

**Or with Podman:**

```bash
podman-compose up
```

After starting the container, Keycloak will be available at **http://localhost:8080**.

#### Multi-Stage Build Process

The Dockerfile uses a two-stage build approach:

| Stage | Base Image | Purpose |
|-------|------------|---------|
| **Builder** | `maven:3.9-eclipse-temurin-21` | Compiles the project with Maven and creates the provider JAR |
| **Runtime** | `quay.io/keycloak/keycloak:26.0.0` | Copies the provider JAR and registers it with Keycloak |

**Build Steps:**
1. Maven compiles the source code (`mvn clean package -DskipTests`)
2. JAR is copied to `/opt/keycloak/providers/`
3. Keycloak configuration is updated (`kc.sh build`)

---

## ⚙️ Configuration

### Email Configuration

Configure SMTP settings in your Keycloak realm to enable email delivery:

1. **Login** as admin to your Keycloak installation
2. **Switch** to your target realm
3. Navigate to **Realm Settings** → **Email** tab
4. Configure your SMTP server settings:
   - SMTP Host
   - SMTP Port
   - From Email Address
   - Authentication credentials (if required)
   - Enable SSL/TLS (recommended)

### Authentication Flow Setup

Create a custom authentication flow with email OTP:

1. Navigate to **Authentication** → **Flows**
2. **Create** a new browser flow (or copy the existing one)
3. Add **Email OTP** authenticator after the **Username Password Form**
4. Set the execution requirement to **Required** or **Alternative**
5. **Bind** the new flow to the browser flow

**Example Flow Configuration:**

![Authentication Flow Example](docs/img/otp-form.png)

### Email Provider Configuration

The authenticator supports multiple email service providers for enhanced flexibility and reliability.

#### Supported Providers

| Provider | Description | Requirements |
|----------|-------------|--------------|
| **Keycloak SMTP** (Default) | Uses Keycloak's built-in SMTP configuration | Realm SMTP settings configured |
| **SendGrid** | SendGrid cloud email service | SendGrid API key and from email |
| **AWS SES** | Amazon Simple Email Service | AWS credentials, region, and verified sender |
| **Mailgun** | Mailgun email delivery service | *Coming soon* |

#### Configure Email Provider in Authentication Flow

1. Navigate to **Authentication** → **Flows**
2. Select your custom flow containing the **Email OTP** authenticator
3. Click the **⚙️ (gear/settings)** icon next to **Email OTP**
4. Configure the provider settings:

**Option 1: Use Keycloak SMTP (Default)**
```
Email Provider: KEYCLOAK
Enable Fallback to Keycloak SMTP: true
```
No additional configuration needed. Uses realm SMTP settings.

**Option 2: Use SendGrid**
```
Email Provider: SENDGRID
SendGrid API Key: SG.xxxxxxxxxxxxxxxxxxxx
SendGrid From Email: noreply@yourdomain.com
SendGrid From Name: Your Company Name  (optional)
Enable Fallback to Keycloak SMTP: true  (recommended)
```

**Option 3: Use AWS SES**
```
Email Provider: AWS_SES
AWS SES Region: us-east-1
AWS Access Key ID: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS SES From Email: noreply@yourdomain.com
AWS SES From Name: Your Company Name  (optional)
Enable Fallback to Keycloak SMTP: true  (recommended)
```

#### SendGrid Setup

1. **Get SendGrid API Key:**
   - Sign up at [SendGrid](https://sendgrid.com/)
   - Navigate to **Settings** → **API Keys** → **Create API Key**
   - Select **Full Access** or **Restricted Access** with **Mail Send** permissions
   - Copy the API key (starts with `SG.`)

2. **Verify Sender Identity:**
   - Navigate to **Settings** → **Sender Authentication**
   - Either verify a single sender email address OR
   - Set up domain authentication for your sending domain

3. **Configure in Keycloak:**
   - Enter the API key in **SendGrid API Key** field
   - Enter your verified sender email in **SendGrid From Email**
   - Optionally set a friendly display name

#### AWS SES Setup

1. **Create IAM User with SES Permissions:**
   - Go to AWS IAM Console → Users → Create User
   - Attach policy: `AmazonSESFullAccess` (or create custom policy with `ses:SendEmail` permission)
   - Create access keys and save the Access Key ID and Secret Access Key

2. **Verify Sender Email or Domain:**
   - Go to AWS SES Console → Verified identities
   - Click **Create identity**
   - For testing: Verify a single email address
   - For production: Verify your domain (requires DNS configuration)
   - Follow email/DNS verification steps

3. **Request Production Access (if needed):**
   - By default, AWS SES starts in **Sandbox mode**
   - In Sandbox, you can only send to verified addresses
   - To send to any email: Request production access in SES Console

4. **Choose AWS Region:**
   - SES is available in specific regions (e.g., us-east-1, eu-west-1)
   - Choose region close to your users for lower latency
   - Note: Verified identities are region-specific

5. **Configure in Keycloak:**
   - Select `AWS_SES` from Email Provider dropdown
   - Enter AWS region (e.g., `us-east-1`)
   - Enter IAM access key ID
   - Enter IAM secret access key
   - Enter verified sender email
   - Optionally set sender display name

#### Fallback Mechanism

When **Enable Fallback to Keycloak SMTP** is enabled (recommended):
- If the primary provider fails, the system automatically falls back to Keycloak's SMTP
- Ensures email delivery reliability even if 3rd party service is unavailable
- Fallback events are logged for monitoring

---

## 💻 Development

### Running Tests

```bash
mvn test
```

> **Note:** Tests may fail on Java 25+ due to Mockito/ByteBuddy compatibility. Use Java 21 for development.

---

## 🧪 Local Testing

For detailed instructions on testing this authenticator locally with Podman or Docker, see:

**[📖 Local Testing Guide](docs/LOCAL_TESTING.md)**

Quick start:
```bash
# Build and run with Podman
podman build -t keycloak-email-auth:latest .
podman run -d --name keycloak-test \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-email-auth:latest start-dev
```

Access admin console at: http://localhost:8080/admin

---

## 📚 Resources

- [Keycloak Server Development Documentation](https://www.keycloak.org/docs/latest/server_development/index.html)
- [Keycloak Official Website](https://www.keycloak.org/)

---

## 🌍 Localization

This project supports multiple languages for a better user experience across different regions.

### Supported Languages

| Language | Code | File | Status |
|----------|------|------|--------|
| 🇬🇧 English | `en` | `messages_en.properties` | ✅ Complete |
| 🇹🇷 Turkish | `tr` | `messages_tr.properties` | ✅ Complete |
| 🇫🇷 French | `fr` | `messages_fr.properties` | ✅ Complete |
| 🇮🇹 Italian | `it` | `messages_it.properties` | ✅ Complete |
| 🇪🇸 Spanish | `es` | `messages_es.properties` | ✅ Complete |
| 🇩🇰 Danish | `da` | `messages_da.properties` | ✅ Complete |
| 🇩🇪 German | `de` | `messages_de.properties` | ✅ Complete |
| 🇷🇺 Russian | `ru` | `messages_ru.properties` | ✅ Complete |
| 🇦🇿 Azerbaijani | `az` | `messages_az.properties` | ✅ Complete |
| 🇸🇦 Arabic | `ar` | `messages_ar.properties` | ✅ Complete |
| 🇹🇼 Chinese (Traditional) | `zh_TW` | `messages_zh_TW.properties` | ✅ Complete |

### How to Add a New Language

We welcome contributions for additional language support! Here's how you can add a new language:

1. **Navigate to the messages directory:**
   ```bash
   cd src/main/resources/theme-resources/messages/
   ```

2. **Create a new properties file:**
   ```bash
   cp messages_en.properties messages_<language_code>.properties
   ```
   Replace `<language_code>` with the appropriate ISO 639-1 language code (e.g., `de` for German, `es` for Spanish, `ja` for Japanese).

3. **Translate the content:**
   Open the new file and translate all text values while keeping the keys unchanged:
   ```properties
   # Example for German (messages_de.properties)
   resendCode=Code erneut senden
   emailOtpForm=Bitte geben Sie den sechsstelligen Code ein, der an Ihre E-Mail gesendet wurde.
   ```

4. **Test your translation:**
   - Build the project: `mvn clean package`
   - Deploy to Keycloak
   - Switch your browser/Keycloak to the new language
   - Verify all messages display correctly

5. **Submit a Pull Request:**
   - Fork the repository
   - Create a feature branch: `git checkout -b add-<language>-translation`
   - Commit your changes: `git commit -m "Add <language> translation"`
   - Push to your fork and submit a PR

### Translation Keys Reference

Here are the main translation keys used in this authenticator:

```properties
resendCode=Resend Code
emailOtpForm=Please enter the six digit code...
emailCodeSubject={0} access code
emailCodeBody=Access code: {0}...
email-authenticator-display-name=Email Authenticator
email-authenticator-help-text=Receive a one-time verification code...
email-authenticator-setup-title=Set up Email Authenticator
email-authenticator-setup-description=Use this option to receive...
email-authenticator-setup-button=Enable Email Authenticator
email-authenticator-setup-cancelled=Email authenticator setup cannot...
email-authenticator-setup-error=We couldn't enable Email Authenticator...
email-authenticator-setup-missing-email=Add an email address...
email-authenticator-resend-cooldown=Please wait {0} seconds...
```

> **Note:** Text inside curly braces like `{0}`, `{1}` are placeholders for dynamic values. Do not translate these.

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Ways to Contribute

1. **🌍 Add New Translations** - Follow the [localization guide](#how-to-add-a-new-language) above
2. **🐛 Report Bugs** - Open an issue with details about the problem
3. **✨ Suggest Features** - Share your ideas via GitHub issues
4. **🔧 Submit Pull Requests** - Fix bugs or implement new features

### Contribution Guidelines

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Standards

- Follow existing code style and conventions
- Add comments for complex logic
- Update documentation when needed
- Test your changes thoroughly

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ for the Keycloak community**
