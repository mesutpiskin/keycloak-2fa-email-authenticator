#!/bin/bash

# Keycloak 2FA Email Authenticator - Local Release Script
# This script builds the JAR file and provides upload instructions

set -e  # Exit on error

echo "🚀 Keycloak 2FA Email Authenticator - Starting Release..."
echo ""

# Version check
if [ -z "$1" ]; then
    echo "❌ Error: You must specify a version!"
    echo "Usage: ./release.sh v1.0.0"
    exit 1
fi

VERSION=$1
echo "📦 Version: $VERSION"
echo ""

# Clean and build
echo "🔨 Starting Maven build (skipping tests)..."
mvn clean package -DskipTests

# Build check
JAR_FILE=$(find target -name "keycloak-2fa-email-authenticator*.jar" -type f)
if [ -z "$JAR_FILE" ]; then
    echo "❌ JAR file not found!"
    exit 1
fi

echo "✅ Build successful: $JAR_FILE"
echo ""

# Create git tag
echo "🏷️  Creating git tag: $VERSION"
git tag -a "$VERSION" -m "Release $VERSION"
git push origin "$VERSION"

echo ""
echo "✅ Release preparation complete!"
echo ""
echo "📋 Next Steps:"
echo "1. Go to this URL on GitHub:"
echo "   https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/releases/new"
echo ""
echo "2. Select '$VERSION' as the tag"
echo ""
echo "3. Upload the following JAR file:"
echo "   $JAR_FILE"
echo ""
echo "4. Add release notes and click 'Publish release'"
echo ""
