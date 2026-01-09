# Local Testing Guide

Comprehensive guide for testing Keycloak Email Authenticator locally using **Docker** or **Podman**.

## Prerequisites

✅ **Docker** OR **Podman** installed  
✅ Java 21 installed  
✅ Maven installed  
✅ Port 8080 available

> **Note:** All commands work with both Docker and Podman. Simply replace `podman` with `docker` or vice versa.

---

## Quick Start (TL;DR)

```bash
# 1. Build the JAR
cd /path/to/keycloak-2fa-email-authenticator
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package

# 2. Build image (Docker or Podman)
docker build -t keycloak-email-auth:latest .
# OR
podman build -t keycloak-email-auth:latest .

# 3. Run container
docker run -d \
  --name keycloak-test \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-email-auth:latest \
  start-dev
# OR
podman run -d \
  --name keycloak-test \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-email-auth:latest \
  start-dev

# 4. View logs
docker logs -f keycloak-test
# OR
podman logs -f keycloak-test

# 5. Access
# http://localhost:8080/admin
# Username: admin, Password: admin
```

---

## Method 1: Docker/Podman Compose

### Step 1: Build JAR

```bash
cd /path/to/keycloak-2fa-email-authenticator

# Build with Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package
```

**Expected output:** `target/keycloak-2fa-email-authenticator-v26.0.0-SNAPSHOT.jar`

### Step 2: Run with Compose

**Docker Compose:**
```bash
docker-compose up --build
```

**Podman Compose:**
```bash
podman-compose up --build
```

**Alternative (use Docker CLI with Compose):**
```bash
docker compose up --build
```

### Step 3: Access Keycloak

Open in browser: **http://localhost:8080**

- **Username:** admin
- **Password:** admin

---

## Method 2: Manual Build (Recommended)

### Step 1: Build Image

**Docker:**
```bash
cd /path/to/keycloak-2fa-email-authenticator
docker build -t keycloak-email-auth:latest .
```

**Podman:**
```bash
cd /path/to/keycloak-2fa-email-authenticator
podman build -t keycloak-email-auth:latest .
```

**This will:**
1. Build JAR with Maven
2. Copy JAR to Keycloak 26.0.0 image
3. Integrate provider into Keycloak

### Step 2: Run Container

**Docker:**
```bash
docker run -d \
  --name keycloak-test \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-email-auth:latest \
  start-dev
```

**Podman:**
```bash
podman run -d \
  --name keycloak-test \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-email-auth:latest \
  start-dev
```

### Step 3: View Logs

**Docker:**
```bash
docker logs -f keycloak-test
```

**Podman:**
```bash
podman logs -f keycloak-test
```

**Successful startup message:**
```
Keycloak 26.0.0 on JVM (powered by Quarkus)
Running the server in development mode. DO NOT use this configuration in production.
```

---

## Keycloak Configuration

### 1. Access Admin Console

http://localhost:8080/admin

- Username: `admin`
- Password: `admin`

### 2. Create Realm (or use Master)

**To create new realm:**
1. Click dropdown in top-left → "Create Realm"
2. Realm name: `test`
3. Click "Create"

### 3. Configure Email Settings

> **IMPORTANT:** SMTP settings required for email sending!

**Realm Settings → Email:**
- **From:** noreply@example.com
- **Host:** smtp.gmail.com (for Gmail)
- **Port:** 587
- **Enable StarTLS:** ON
- **Username:** your-email@gmail.com
- **Password:** your-app-password

**For Gmail:**
- Create "App Password": https://myaccount.google.com/apppasswords
- 2FA must be enabled

### 4. Create Test User

**Users → Add User:**
- Username: `testuser`
- Email: `your-email@gmail.com` (real email!)
- Email Verified: ON
- Click "Create"

**Credentials → Set Password:**
- Password: `test123`
- Temporary: OFF

### 5. Create Authentication Flow

#### a) Copy Flow

**Authentication → Flows:**
1. Select "Browser" flow
2. Click "Copy" → Name: "Browser with Email OTP"
3. Click "OK"

#### b) Add Email OTP

In "Browser with Email OTP" flow:
1. Select "Browser with Email OTP Forms" row
2. Click "Add step"
3. Select **"Email OTP"** (our provider!)
4. Click "Add"

#### c) Set Requirement

On "Email OTP" row:
- Requirement: **REQUIRED** (or CONDITIONAL)

#### d) Activate Flow

**Bindings tab:**
- Browser Flow: Select "Browser with Email OTP"
- Click "Save"

### 6. Configure Email OTP Settings

Click **⚙️ (Settings)** icon on "Email OTP" row:

- **Code length:** 6
- **Time-to-live:** 300 (5 minutes)
- **Simulation mode:** false (for real emails)
- **Resend cooldown:** 30 (seconds)

---

## Test Scenarios

### Test 1: Basic Email OTP Flow

1. **Logout** (if logged in)
2. Go to login: http://localhost:8080/realms/test/account
3. Username: `testuser`, Password: `test123`
4. ✅ Code will be sent to email
5. Enter code
6. ✅ Login successful

### Test 2: Code Expiration

1. Login, wait for code
2. Wait 5+ minutes
3. Enter expired code
4. ❌ Should see "Code expired" error

### Test 3: Resend Code

1. Login, receive email
2. Click "Resend code" link
3. Try again within 30 seconds → cooldown message
4. After 30 seconds → new code sent

### Test 4: Invalid Code

1. Login
2. Enter wrong code
3. ❌ Should see "Invalid code" error

### Test 5: Simulation Mode (Development)

**To test without sending emails:**

In flow settings:
- Simulation mode: **true**

View code in container logs:
```bash
podman logs -f keycloak-test | grep "SIMULATION MODE"
```

Output:
```
***** SIMULATION MODE ***** Email code send to test@example.com for user testuser is: 123456
```

### Test 6: Language Support (11 Languages)

Change language on login page:
- 🇹🇷 Türkçe
- 🇬🇧 English  
- 🇩🇪 Deutsch
- 🇫🇷 Français
- 🇮🇹 Italiano
- 🇪🇸 Español
- 🇷🇺 Русский
- 🇸🇦 العربية
- 🇩🇰 Dansk
- 🇦🇿 Azərbaycan
- 🇹🇼 中文 (繁體)

---

## Debugging & Troubleshooting

### Container Not Running

```bash
# Check container status
podman ps -a

# Check logs
podman logs keycloak-test

# Stop and remove
podman stop keycloak-test
podman rm keycloak-test
```

### Provider Not Loaded

**Check inside container:**
```bash
podman exec -it keycloak-test ls -la /opt/keycloak/providers/
```

**Expected:**
```
keycloak-2fa-email-authenticator-v26.0.0-SNAPSHOT.jar
```

### Email Not Sending

1. **Test SMTP settings:**
   - Realm Settings → Email → "Test connection"

2. **Enable simulation mode:**
   - Flow settings → Simulation mode: true
   - Watch logs: `podman logs -f keycloak-test`

3. **For Gmail:**
   - Use App Password (not regular password)
   - 2FA must be enabled

### Port 8080 Already in Use

```bash
# Use different port
podman run -d \
  -p 8081:8080 \
  ...
```

Access at: http://localhost:8081

---

## Container Management

All commands work with both `docker` and `podman`. Examples show both.

### Stop Container
```bash
docker stop keycloak-test
# OR
podman stop keycloak-test
```

### Start Container
```bash
docker start keycloak-test
# OR
podman start keycloak-test
```

### Remove Container
```bash
docker stop keycloak-test && docker rm keycloak-test
# OR
podman stop keycloak-test && podman rm keycloak-test
```

### Remove Image
```bash
docker rmi keycloak-email-auth:latest
# OR
podman rmi keycloak-email-auth:latest
```

### Rebuild After Code Changes

```bash
# 1. Rebuild JAR
mvn clean package

# 2. Rebuild image
docker build -t keycloak-email-auth:latest .
# OR
podman build -t keycloak-email-auth:latest .

# 3. Recreate container
docker stop keycloak-test && docker rm keycloak-test
docker run -d --name keycloak-test \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  keycloak-email-auth:latest start-dev
```

---

## Production Deployment

> **WARNING:** `start-dev` should **NOT** be used in production!

### For Production:

```bash
podman run -d \
  --name keycloak-prod \
  -p 8443:8443 \
  -e KC_HOSTNAME=keycloak.example.com \
  -e KC_HTTPS_CERTIFICATE_FILE=/path/to/cert.pem \
  -e KC_HTTPS_CERTIFICATE_KEY_FILE=/path/to/key.pem \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://db/keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=password \
  keycloak-email-auth:latest \
  start
```

---

## References

- **Keycloak Documentation:** https://www.keycloak.org/docs/latest/
- **Podman Documentation:** https://docs.podman.io/
- **Project README:** [README.md](../README.md)
- **Keycloak SPI Guide:** https://www.keycloak.org/docs/latest/server_development/

---

## Support

If you encounter issues:
1. Check container logs
2. Review Keycloak Admin Console error messages
3. Open GitHub Issue: https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/issues
