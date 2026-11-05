# 🚀 Release Guide

This document explains how to publish new versions of the Keycloak 2FA Email Authenticator project.

## Release Flow

Releases are a two-step, PR-reviewed flow:

1. Run **Actions → Prepare release → Run workflow** (`prepare-release.yml`), entering the version
   to release (e.g. `26.5.1`, no leading `v`, no `-KC` suffix). It bumps `pom.xml` on a
   `release/v<version>` branch (always cut from `main`) and opens a `Release <version>` PR.
   Nothing is tagged or published yet.
2. Review and merge that PR. Merging runs `release.yml` (`Release`), which tags `v<version>` and
   creates a GitHub Release with auto-generated notes, then runs `publish.yml`
   (`Publish packages`) to build and publish the jars for the last 10 Keycloak releases to Maven
   Central and GitHub Packages, and attach them to the release.
3. Verify publication on Maven Central, GitHub Packages and the GitHub Release page.

Safety checks: the tag/publish step refuses to run unless `pom.xml` at the merge commit matches
the version in the `release/v<version>` branch name, so an unrelated branch with that prefix
cannot trigger a release.

## New Keycloak Versions

A new Keycloak release does not need a new release of this project. Each jar is published as
`<version>-KC<keycloak-version>`, so an existing tag can be built and published again for a new
Keycloak version.

- **Weekly, automatically:** `check-keycloak-releases.yml` runs every Monday. It publishes the
  latest release for every Keycloak version released after it (by date, so a patch on an older
  Keycloak line counts too). If the latest release has no jars at all (its own publish failed or
  is still running), it publishes nothing and logs a warning. It can also be run by hand from
  **Actions → Check for new Keycloak releases**. GitHub disables scheduled workflows after 60
  days without repository activity; if that happens, re-enable it from the Actions tab.
- **By hand:** run **Actions → Publish packages → Run workflow**. Leave `tag` empty for the
  latest release, and set `keycloak_versions` (e.g. `26.8.1,26.8.2`), or leave it empty for the
  last 10 Keycloak releases. Use this to publish a new Keycloak version between weekly runs, or
  to fill an older gap the weekly check leaves alone.

Published jars are never rebuilt or replaced. A Keycloak version whose jar is already attached
to the release is skipped. A version already on Maven Central but missing from the release (for
example after a failed run) is not rebuilt: Central's signed files are attached instead.

Each build runs the tests against that Keycloak version. If they fail, nothing is published for
it, and the code needs a fix and a normal release. The weekly check retries a failing new
version every week until then.

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
