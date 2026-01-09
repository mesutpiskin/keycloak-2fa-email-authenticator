# Release Notes - v26.1.0 🚀

## 📧 Major Feature: Multiple Email Provider Support

This release introduces a powerful email service abstraction layer, enabling seamless integration with multiple 3rd party email providers while maintaining full backward compatibility.

---

## ✨ New Features

### 🔌 Email Provider Abstraction (Strategy Pattern)
- **Architecture**: Clean, extensible email service abstraction using Strategy Pattern
- **Multiple Providers**: Support for Keycloak SMTP, SendGrid, AWS SES, and Mailgun (coming soon)
- **Runtime Selection**: Choose email provider via Keycloak Admin UI configuration
- **Zero Breaking Changes**: Fully backward compatible with existing installations

### 📮 SendGrid Integration
- **Production Ready**: Full SendGrid REST API v3 integration
- **API Key Authentication**: Secure authentication using SendGrid API keys
- **Verified Sender Support**: Use verified email addresses or domains
- **HTML & Text Support**: Automatic content type handling
- **Error Handling**: Comprehensive error logging and status code validation

**Configuration:**
```
Email Provider: SENDGRID
SendGrid API Key: SG.xxxxxxxxxxxx
SendGrid From Email: noreply@yourdomain.com
```

### ☁️ AWS SES Integration  
- **Production Ready**: AWS SDK v2 integration with Amazon Simple Email Service
- **IAM Credentials**: Secure authentication using AWS Access Keys
- **Regional Support**: Deploy in any AWS SES-supported region (us-east-1, eu-west-1, etc.)
- **Sandbox & Production**: Works in both SES Sandbox and Production modes
- **Verified Identities**: Support for verified email addresses and domains

**Configuration:**
```
Email Provider: AWS_SES
AWS SES Region: us-east-1
AWS Access Key ID: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS SES From Email: noreply@yourdomain.com
```

### 🔄 Automatic Fallback Mechanism
- **High Availability**: Automatically falls back to Keycloak SMTP if primary provider fails
- **Configurable**: Enable/disable fallback via Admin UI
- **Logging**: Detailed logs for monitoring provider failures and fallback events
- **Reliability**: Ensures email delivery even when 3rd party services are unavailable

---

## 🏗️ Technical Implementation

### New Components

**Core Abstraction:**
- `EmailSender` interface - Strategy pattern contract
- `EmailMessage` model - Immutable email message with Builder pattern
- `EmailProviderType` enum - Provider type enumeration
- `EmailSenderFactory` - Provider instantiation factory

**Provider Implementations:**
- `KeycloakEmailSender` - Default SMTP (backward compatible)
- `SendGridEmailSender` - SendGrid REST API integration
- `AwsSesEmailSender` - AWS SES SDK v2 integration

**Configuration:**
- Added 11+ new configuration constants for provider settings
- Enhanced Admin UI with provider selection dropdown
- Password-masked fields for API keys and secrets

### Dependencies

**Added:**
- `sendgrid-java:4.10.2` - SendGrid Java SDK
- `aws-sdk-ses:2.20.26` - AWS SDK for SES

**Package Size Impact:** ~5MB added for provider SDKs

---

## 📊 Statistics

| Metric | Count |
|--------|-------|
| New Java Files | 7 |
| Modified Java Files | 3 |
| New Lines of Code | ~900 |
| Dependencies Added | 2 |
| Supported Providers | 3 active (Keycloak, SendGrid, AWS SES) |
| Configuration Properties | 17 total |

---

## 🚀 Upgrade Guide

### For Existing Users

**No action required!** This release is fully backward compatible:
- Default provider remains `KEYCLOAK` (uses existing realm SMTP settings)
- No configuration changes needed
- Existing functionality unchanged

### To Use New Providers

1. **Update JAR:**
   ```bash
   cp keycloak-2fa-email-authenticator-v26.1.0.jar /opt/keycloak/providers/
   /opt/keycloak/bin/kc.sh build
   ```

2. **Configure Provider:**
   - Navigate to **Authentication** → **Flows** → Email OTP Settings
   - Select desired provider from dropdown
   - Enter provider-specific credentials
   - Enable fallback (recommended)

3. **Verify Setup:**
   - Test authentication flow
   - Check Keycloak logs for successful email delivery

---

## 📖 Documentation

### Updated Sections
- **Email Provider Configuration** - New comprehensive section
- **SendGrid Setup Guide** - Step-by-step instructions
- **AWS SES Setup Guide** - Complete AWS configuration walkthrough
- **Features List** - Updated with new capabilities
- **Fallback Mechanism** - Reliability documentation

### Examples
- Configuration examples for all 3 providers
- SendGrid account setup and API key generation
- AWS IAM user creation and SES verification
- Sandbox vs Production mode explanation

---

## 🔧 Configuration Examples

### SendGrid with Fallback
```
Email Provider: SENDGRID
SendGrid API Key: SG.abc123xyz...
SendGrid From Email: noreply@example.com
SendGrid From Name: Example Corp
Enable Fallback: true
```

### AWS SES in Production
```
Email Provider: AWS_SES
AWS SES Region: eu-west-1
AWS Access Key ID: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key: wJalrXUtnFEMI/K7MDENG/***
AWS SES From Email: noreply@example.com
Enable Fallback: true
```

---

## 🐛 Bug Fixes

- None in this release (new feature release)

---

## ⚠️ Breaking Changes

- **None** - Fully backward compatible

---

## 🔮 Coming Soon

- **Mailgun Integration** - Mailgun API support
- **SMTP Templates** - Custom email templates for each provider
- **Analytics Integration** - SendGrid analytics and AWS SES metrics
- **Bulk Sending** - Optimizations for high-volume scenarios

---

## 🙏 Credits

Special thanks to the Keycloak community for feature requests and feedback.

---

## 📦 Download

**JAR File:** [`keycloak-2fa-email-authenticator-v26.1.0.jar`](../../releases/download/v26.1.0/keycloak-2fa-email-authenticator-v26.1.0.jar)

**Checksum (SHA-256):** Will be generated upon release

---

## 🔗 Links

- [SendGrid Documentation](https://docs.sendgrid.com/)
- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)
- [Keycloak Server Development Guide](https://www.keycloak.org/docs/latest/server_development/)

---

**Full Changelog**: [v26.0.0...v26.1.0](../../compare/v26.0.0...v26.1.0)
