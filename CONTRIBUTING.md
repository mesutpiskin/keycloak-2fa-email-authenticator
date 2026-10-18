# Contributing to keycloak-2fa-email-authenticator

First of all, thank you for your interest in contributing to **keycloak-2fa-email-authenticator**! 🎉

Contributions of all kinds are welcome: bug fixes, new email providers, translations, documentation improvements, tests, and more. Every contribution helps make the project better.

## Ways to Contribute

You can contribute by:

- Reporting bugs
- Suggesting new features or email providers
- Adding or improving translations of the login and email messages
- Improving the documentation website
- Fixing bugs
- Adding or improving tests
- Refactoring existing code
- Reviewing pull requests

## Before You Start

Before working on a contribution, please:

1. Search existing issues and pull requests to avoid duplicate work.
2. For significant changes, such as a new email provider, a new configuration option, or a change to the authentication or enrollment flow, open a **Feature Request** using the GitHub issue template.
3. Wait for feedback before starting a large implementation.

Small bug fixes, documentation improvements, translations, and typo fixes can generally be submitted directly as a pull request.

## Development Setup

### Prerequisites

Make sure you have the following installed:

- JDK 21 (for example [Eclipse Temurin](https://adoptium.net/))
- Maven 3.9 or newer
- Git
- Docker and Docker Compose (optional, for testing against a local Keycloak)
- Node.js 22 and pnpm (optional, only to work on the documentation website)

### 1. Fork the Repository

Fork this repository to your GitHub account.

### 2. Clone Your Fork

```bash
git clone https://github.com/<your-github-username>/keycloak-2fa-email-authenticator.git
cd keycloak-2fa-email-authenticator
```

### 3. Configure the Upstream Remote

Add the original repository as the upstream remote.

```bash
git remote add upstream https://github.com/mesutpiskin/keycloak-2fa-email-authenticator.git
```

Verify your remotes:

```bash
git remote -v
```

### 4. Build the Project

```bash
mvn -B package
```

This compiles the plugin, runs the unit tests, and builds the provider JAR under `target/`.

## Project Layout

| Path | Contents |
| ---- | -------- |
| `src/main/java/.../email` | Authenticators (Email OTP, Conditional, No Enrollment), required action, credential provider and their factories |
| `src/main/java/.../email/service` | `EmailSender` interface and `EmailSenderFactory` |
| `src/main/java/.../email/service/impl` | Built-in email senders (Keycloak SMTP, SendGrid, AWS SES, Mailgun) |
| `src/main/resources/theme-resources/templates` | Login page templates and the code email (`html/` and `text/`) |
| `src/main/resources/theme-resources/messages` | Message bundles (`messages_<locale>.properties`) |
| `src/main/resources/theme/email-code-theme` | `email-code-theme`, an email theme bundled with the plugin |
| `src/main/resources/META-INF/services` | `ServiceLoader` registrations for the Keycloak SPIs |
| `src/test/java` | Unit tests |
| `website/` | Docusaurus documentation website |
| `docs/` | Maintainer notes (release process, local testing) |

## Development Workflow

### Keep Your Fork Up to Date

Before starting new work, synchronize your fork with the upstream repository.

```bash
git checkout main
git fetch upstream
git merge upstream/main
git push origin main
```

### Create a Branch

Create a new branch from the latest `main` branch.

```bash
git checkout -b feat/your-feature-name
```

Use a descriptive branch name, for example:

- `feat/postmark-sender`
- `fix/otp-resend-cooldown`
- `docs/aws-ses-setup`
- `i18n/dutch-messages`
- `test/sendgrid-sender`

### Make Your Changes

Please:

- Keep changes focused on a single concern.
- Follow the existing project architecture and coding style.
- Write clean, readable, and maintainable code.
- Add or update tests when appropriate.
- Update the documentation (`README.md` and `website/docs`) when configuration or behavior changes.
- Never commit real credentials, API keys, or email addresses, including in tests and examples. Use `example.com` addresses.

### Validate Your Changes

Before opening a pull request, make sure the full build passes:

```bash
mvn -B package
```

### Compatibility Checks

The release publishes a JAR for each of the last 10 Keycloak releases, not only the version in `pom.xml`. A change that compiles against the default Keycloak version can still break against an older one. CI builds against both the default and the oldest published version on every pull request; to reproduce the second check locally, run:

```bash
mvn -B package -Dkeycloak.version=<keycloak-version>
```

See [`COMPATIBILITY.md`](COMPATIBILITY.md) for the supported Keycloak versions.

### Test Against a Local Keycloak

To try your change in a running Keycloak, build and start the bundled Docker setup:

```bash
docker compose up --build
```

Keycloak is then available at <http://localhost:8080> (admin / admin). Enable **Simulation mode** (`simulationMode`) on the authenticator to validate the flow without sending real emails: the code is written to the Keycloak log instead. See the [local testing guide](https://mesutpiskin.github.io/keycloak-2fa-email-authenticator/local-testing) for more details.

### Adding an Email Provider

Email providers implement `com.mesutpiskin.keycloak.auth.email.service.EmailSender`. To add one:

1. Add the implementation under `service/impl`.
2. Add a value to `EmailProviderType` and return your sender from `EmailSenderFactory`.
3. Add its configuration keys to `EmailConstants` and the authenticator configuration properties in `AbstractEmailAuthenticatorFormFactory`.
4. Validate the required configuration and fail with a descriptive message when a value is missing.
5. Add unit tests next to the existing sender tests.
6. Document the provider, its configuration keys, and setup steps in the README and the [email providers page](website/docs/configuration/email-providers.mdx).

The plugin JAR does not bundle third-party libraries. Prefer the JDK's `java.net.http.HttpClient`, as `MailgunEmailSender` does, over adding a provider SDK.

### Adding a Translation

Copy `src/main/resources/theme-resources/messages/messages_en.properties` to `messages_<locale>.properties`, translate the values, and keep the keys unchanged. Then add the language to the [localization](website/docs/localization.mdx) and [contributing](website/docs/contributing.mdx) pages of the website.

### Working on the Documentation Website

```bash
cd website
pnpm install
pnpm start
```

Run `pnpm build` before opening a pull request to catch broken links.

## Commit Messages

This project follows the **Conventional Commits** specification.

Using Conventional Commits helps us:

- Generate release notes automatically.
- Maintain a clear and searchable Git history.
- Support the release process.

Use the appropriate commit type for your changes.

| Type       | Description                                 |
| ---------- | ------------------------------------------- |
| `feat`     | Introduces a new feature                    |
| `fix`      | Fixes a bug                                 |
| `docs`     | Documentation-only changes                  |
| `refactor` | Code improvements without changing behavior |
| `test`     | Adds or updates tests                       |
| `build`    | Changes to the build system or dependencies |
| `ci`       | Changes to CI/CD workflows                  |
| `chore`    | Project maintenance tasks                   |

Examples:

```text
feat: add Postmark email sender
fix: enforce max code attempts when brute force protection is on
docs: document the AWS SES sandbox limits
refactor: simplify email sender selection
test: cover SendGrid sender error handling
build: bump Keycloak to 26.8.1
ci: build against the oldest published Keycloak
```

## Pull Requests

Before submitting a pull request, please make sure that:

- `mvn -B package` passes.
- The change builds against the oldest supported Keycloak version when it touches Keycloak APIs.
- Documentation has been updated when necessary.
- Your pull request focuses on a single logical change.

When opening a pull request, fill in the pull request template:

- Provide a clear summary of your changes.
- Explain the motivation behind the change.
- Link the related issue when applicable (for example, `Closes #123`).
- Mention any breaking changes, such as renamed configuration keys, provider IDs, or message keys.

Please respond promptly to review feedback to help keep the review process efficient.

## Reporting Bugs

If you've found a bug, please use the **Bug Report** issue template.

Before creating a new issue, search existing issues and check the [troubleshooting guide](https://mesutpiskin.github.io/keycloak-2fa-email-authenticator/troubleshooting). The template will guide you through providing the information needed to investigate the problem, such as your Keycloak and plugin versions, authenticator, email provider, and server logs.

Please remove credentials, OTP codes, email addresses, and other personal data from logs before posting them.

## Feature Requests

Have an idea for a new feature, provider, or enhancement?

Please use the **Feature Request** issue template and clearly describe:

- The problem you're trying to solve.
- Your proposed solution.
- Alternative approaches you've considered.
- Any additional context that may help.

## Accessibility

If something in the login pages, the code email, or the documentation is hard or impossible to use, please use the **Accessibility Issue** template. When you change a login template, the code email, or a message bundle, follow the contributor expectations in [**ACCESSIBILITY.md**](./ACCESSIBILITY.md).

## Security

If you discover a security vulnerability, **do not open a public GitHub issue**.

Please follow the instructions in [**SECURITY.md**](./SECURITY.md) to report it privately.

## Code of Conduct

By participating in this project, you agree to follow our [**Code of Conduct**](./CODE_OF_CONDUCT.md).

Please help us maintain a welcoming, respectful, and inclusive community.

## Questions

If you have a question that is not covered by the [documentation](https://mesutpiskin.github.io/keycloak-2fa-email-authenticator/), open an issue using the **Question** template.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).

Thank you for contributing to **keycloak-2fa-email-authenticator**. Your contributions help improve the project for everyone.
