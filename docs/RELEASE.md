# 🚀 Release Guide

This document explains how to publish new versions of the Keycloak 2FA Email Authenticator project.

## Prerequisites

- Maven must be installed
- Git must be configured
- You must have push access to the GitHub repository

## Local Release Process

### Method 1: FULLY AUTOMATED (GitHub CLI) - Easiest! 🚀

If GitHub CLI is installed and you're logged in, **everything is handled with one command**:

```bash
./scripts/release-auto.sh v1.0.0 "Bug fixes and improvements"
```

The script **automatically**:
- ✅ Builds JAR with Maven
- ✅ Creates and pushes git tag
- ✅ Creates GitHub Release
- ✅ Uploads JAR file
- ✅ Adds release notes
- ✅ Shows download URL

**FIRST TIME SETUP:**
```bash
# 1. Login to GitHub CLI (one time only)
gh auth login

# 2. Now you can release with one command!
./scripts/release-auto.sh v1.0.0
```

### Method 2: Semi-Automated (without GitHub CLI)

1. **Run the release script:**
   ```bash
   ./scripts/release.sh v1.0.0
   ```
   
   The script does:
   - ✅ Builds JAR with Maven
   - ✅ Creates and pushes git tag
   - ✅ Shows next steps

2. **Create release on GitHub:**
   - Go to: https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/releases/new
   - Select the tag you created (e.g., `v1.0.0`)
   - Upload the JAR file from the `target/` directory
   - Add release notes
   - Click "Publish release"

### Method 3: Manual

1. **Build the JAR:**
   ```bash
   mvn clean package
   ```

2. **Create git tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **Manual GitHub release:**
   - Go to GitHub repo page
   - `Releases` > `Create new release`
   - Select tag and upload JAR

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
- [ ] Tag created
- [ ] JAR uploaded to GitHub
- [ ] Release notes added
