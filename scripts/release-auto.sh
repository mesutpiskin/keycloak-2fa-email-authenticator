#!/bin/bash

# Keycloak 2FA Email Authenticator - FULLY AUTOMATED Release Script
# This script handles JAR build, tag creation, and GitHub Release in one command

set -e  # Exit on error

echo "🚀 Keycloak 2FA Email Authenticator - Starting Automated Release..."
echo ""

# Version check
if [ -z "$1" ]; then
    echo "❌ Error: You must specify a version!"
    echo "Usage: ./release-auto.sh v1.0.0 \"Release notes (optional)\""
    exit 1
fi

VERSION=$1
RELEASE_NOTES=${2:-"Release $VERSION"}

echo "📦 Version: $VERSION"
echo "📝 Release Notes: $RELEASE_NOTES"
echo ""

# GitHub CLI check
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed!"
    echo "To install: brew install gh"
    echo "Alternative: Use ./release.sh (manual upload)"
    exit 1
fi

# GitHub auth check
if ! gh auth status &> /dev/null; then
    echo "❌ You are not logged into GitHub!"
    echo "To login, run: gh auth login"
    exit 1
fi

# Clean and build
echo "🔨 Starting Maven build (skipping tests)..."
mvn clean package -DskipTests

# Build check
JAR_FILE=$(find target -name "keycloak-2fa-email-authenticator*.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ JAR file not found!"
    exit 1
fi

echo "✅ Build successful: $JAR_FILE"
echo ""

# Git tag check - ask if tag exists
if git rev-parse "$VERSION" >/dev/null 2>&1; then
    echo "⚠️  Tag '$VERSION' already exists!"
    read -p "Do you want to continue? Existing tag will be used. (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Cancelled."
        exit 1
    fi
else
    # Create git tag
    echo "🏷️  Creating git tag: $VERSION"
    git tag -a "$VERSION" -m "Release $VERSION"
    git push origin "$VERSION"
    echo "✅ Tag created and pushed"
fi

echo ""

# Create GitHub Release
echo "📤 Creating GitHub Release..."
gh release create "$VERSION" \
    "$JAR_FILE" \
    --title "Keycloak 2FA Email Authenticator $VERSION" \
    --notes "$RELEASE_NOTES"

echo ""
echo "✅ Release completed successfully!"
echo ""
echo "🌐 Release URL:"
gh release view "$VERSION" --web || echo "   https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/releases/tag/$VERSION"
echo ""
echo "📥 Download URL:"
echo "   https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/releases/download/$VERSION/keycloak-2fa-email-authenticator-v$VERSION.jar"
echo ""
