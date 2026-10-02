# 🚀 Release Guide

This document explains how to publish new versions of the Keycloak 2FA Email Authenticator project.

## Release Flow

Releases are a two-step, PR-reviewed flow driven by `.github/workflows/release.yml`
(`Publish packages`):

1. Run the workflow via **Actions → Publish packages → Run workflow**, entering the version to
   release (e.g. `26.5.1`, no leading `v`, no `-KC` suffix). It bumps `pom.xml` on a
   `release/v<version>` branch (always cut from `main`) and opens a `Release <version>` PR.
   Nothing is tagged or published yet.
2. Review and merge that PR. Merging tags `v<version>`, creates a GitHub Release with
   auto-generated notes, then builds and publishes the jars for the last 10 Keycloak releases to
   Maven Central and GitHub Packages, and attaches them to the release.
3. Verify publication on Maven Central, GitHub Packages and the GitHub Release page.

Safety checks: the tag/publish step refuses to run unless `pom.xml` at the merge commit matches
the version in the `release/v<version>` branch name, so an unrelated branch with that prefix
cannot trigger a release.

### Required GitHub Secrets

- `CENTRAL_TOKEN_USERNAME`
- `CENTRAL_TOKEN_PASSWORD`
- `GPG_SIGNING_KEY`
- `GPG_SIGNING_KEY_PASSWORD`

Optional: `RELEASE_TOKEN` — a PAT with `contents` and `pull-requests` write, used instead of the
default `GITHUB_TOKEN`. Without it the release PR is opened by `GITHUB_TOKEN`, which cannot
trigger the `Java CI with Maven` checks on that PR (a GitHub Actions limitation). Only matters if
`main` requires status checks to merge.

> The `scripts/release*.sh` helpers tag and push directly, bypassing the PR review, and no
> longer trigger publishing. Use the workflow instead.

## Version Numbering

Use Semantic Versioning:
- **Major (v2.0.0)**: Breaking changes
- **Minor (v1.1.0)**: New features (backward compatible)
- **Patch (v1.0.1)**: Bug fixes

## Release Notes Template

```markdown
## What's Changed
- Feature 1 description
- Bug fix description

## Installation
Download the JAR and place it in your Keycloak `providers/` directory.

## Compatibility
- Keycloak 26.0.0+
- Java 21+
```

## Checklist

- [ ] Tests passing (`mvn test`)
- [ ] README up to date
- [ ] Version number correct
- [ ] CHANGELOG updated (if applicable)
- [ ] Release PR reviewed and merged (tags, publishes and uploads the JARs automatically)
- [ ] Release notes checked on the GitHub Release
