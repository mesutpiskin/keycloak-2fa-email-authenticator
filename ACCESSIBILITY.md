# Accessibility

This plugin adds a step to the Keycloak login: after entering their password, a person receives a one-time code by email and types it into a login page. If that page or the code email does not work for someone, they cannot sign in to the application behind it. We want everyone who is asked for a code to be able to receive it and enter it, whatever device, input method, or assistive technology they use.

This document covers the pages the plugin adds to Keycloak, the email it sends, and the documentation website. It explains what we aim for, what contributors should check, how to report a barrier, and the limitations we know about.

## Priorities

The plugin renders three pages inside the Keycloak login theme:

- the page where a person sets up the email authenticator,
- the page where they confirm that setup with a code sent to their email address,
- the page where they enter the code at login.

For these pages and the code email, we prioritize:

- **Keyboard use:** every field and button can be reached and used with the keyboard alone, in a logical order.
- **Screen readers:** every field has a visible label, errors are announced, and buttons have clear names.
- **Time:** people have enough time to receive and type the code. The code lifetime, resend cooldown, and number of attempts are configurable so administrators can adjust them for their users.
- **Clear language:** instructions and error messages say what happened and what to do next, and can be translated.
- **Email content:** the code email is short, is sent as both HTML and plain text, and contains the code as text, so it can be read aloud or copied.

The pages are built on the Keycloak login theme and inherit its styles, colors, and layout. Accessibility of the surrounding page, such as contrast, zoom, and the language switcher, depends on the Keycloak theme in use. The layout of the code email depends on the Keycloak email theme in use.

We aim for these pages to meet [WCAG 2.2](https://www.w3.org/TR/WCAG22/) level AA. This is a goal, not a conformance claim: the plugin has not been formally evaluated.

## Contributor expectations

When you change a login template or the code email (`src/main/resources/theme-resources/templates`), or a message bundle (`src/main/resources/theme-resources/messages`), please:

- Keep a visible `<label>` connected to every input with `for` and `id`.
- Keep errors in text, not only in color, and keep them announced to screen readers.
- Check that the page can be completed with the keyboard alone and that focus is visible.
- Use the Keycloak theme properties (`properties.kc*Class`) rather than custom styles, so the page follows the active theme.
- Add every new user-facing text to `messages_en.properties` instead of hardcoding it, so it can be translated.
- Keep the HTML and plain text versions of the code email in sync, with the code as text rather than an image.
- Write messages in plain language and avoid instructions that depend on position or color, such as "click the green button".

In the pull request, describe how you tested the change, for example with keyboard only, with a screen reader (name it), or at 200% zoom. Screenshots are welcome but not required. There are no automated accessibility checks in CI yet.

## Reporting accessibility issues

If something in the login pages, the code email, or the documentation stops you or someone else from completing a task, please [open an accessibility issue](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/issues/new?template=accessibility.yml).

It helps to include:

- what you were trying to do, for example "enter the code at login",
- what happened and what you expected,
- the browser, email app, and operating system,
- any assistive technology you use, such as a screen reader, magnifier, or voice control,
- the plugin and Keycloak versions, if you know them.

Screenshots or recordings are optional. You do not need to tell us about any disability.

If you are a user of an application that uses this plugin and cannot sign in, please also contact that application's support team: they control the Keycloak configuration and can help you right away.

### Severity

Maintainers assign a severity during triage; reporters do not need to.

- **Blocker:** a person cannot complete login or setup. For example, the code field cannot be reached with the keyboard, or the code expires before a screen reader user can enter it with the default settings.
- **Major:** the task can be completed, but only with significant difficulty or a workaround. For example, an error is shown but not announced.
- **Minor:** the task is harder than it should be, but still possible. For example, unclear wording in an instruction.

### How we respond

This project is maintained by volunteers, so we cannot promise fixed timelines. We will:

- acknowledge the report and ask questions if anything is unclear,
- suggest a workaround when one exists, such as a configuration change,
- prioritize blockers over other work,
- link the fix to the issue and ask you to confirm it works, if you would like to.

## Ownership and maintenance

The project maintainers ([@mesutpiskin](https://github.com/mesutpiskin)) are responsible for accessibility: triaging reports, reviewing pull requests that change the login pages or the code email, and keeping this document up to date. We review it when the templates change significantly and when Keycloak changes its login or email theme.

## Supported environments

The pages are rendered by Keycloak and follow the browsers supported by the Keycloak version you run. They are expected to work with the keyboard, touch, and screen readers through standard HTML form controls.

We have not tested specific combinations of browsers, email apps, and assistive technologies. If you have, please let us know so we can list them here.

## Known limitations

- **The code field is a plain text field with autofill turned off.** It does not tell the browser that it expects a one-time code, so the code is not offered for automatic filling, and phones show a full keyboard instead of a number pad. People must type or paste the code themselves.
- **Field errors are not connected to the field.** On the code pages, the error is announced when it appears, but screen readers do not read it again when moving back to the field.
- **The code expires.** With the default settings (5 minutes), people who need more time to switch apps, find the email, or type may run out of time. Administrators can increase the code time-to-live (`ttl`) and lower the resend cooldown (`resendCooldown`).
- **The expiry is given in seconds.** The code email says the code expires "within 300 seconds" rather than "within 5 minutes", which is harder to understand.
- **Email can be slow or filtered.** The code may arrive late or land in a spam folder, which uses up the time to enter it. Administrators can check their email provider setup and enable fallback to Keycloak SMTP.
- **Codes arrive only by email.** People who cannot access their mailbox during login cannot receive a code. Administrators can offer another second factor in the Keycloak authentication flow.
- **Translations:** messages are available in Arabic, Azerbaijani, Chinese (Traditional), Danish, English, French, German, Italian, Portuguese, Romanian, Russian, Spanish, and Turkish. Other languages fall back to English.

These limitations are open for contributions.

## Feedback and improvements

Suggestions to improve this document or the project's accessibility practices are welcome as an issue or pull request. Please report active barriers through the [reporting process](#reporting-accessibility-issues) above.
