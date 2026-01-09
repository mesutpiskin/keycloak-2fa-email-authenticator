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
- Send OTP codes via email through SMTP
- Configurable email templates
- Conditional authentication support
- Integration with Keycloak's authentication flow builder

---

## ✨ Features

- ✅ Email-based OTP authentication
- ✅ Customizable email templates
- ✅ Conditional authentication flows
- ✅ Multi-stage Docker build support
- ✅ Compatible with Keycloak 26.x
- ✅ Easy integration with existing authentication flows

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
