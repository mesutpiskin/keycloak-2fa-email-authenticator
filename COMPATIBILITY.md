# Keycloak Version Compatibility

Releases are published as `<plugin-version>-KC<keycloak-version>`: each release builds, tests, and publishes one JAR for each of the **last 10 Keycloak releases** at the time of the release. Use the JAR whose `-KC` suffix matches your Keycloak version.

For example, `26.7.2` was published for Keycloak 26.6.2 through 26.8.0. The exact list for a release is on its [GitHub Release page](https://github.com/mesutpiskin/keycloak-2fa-email-authenticator/releases) and on [Maven Central](https://central.sonatype.com/artifact/io.github.mesutpiskin/keycloak-2fa-email-authenticator/versions).

| Keycloak version | Status |
|---|---|
| One of the last 10 releases when the plugin was released | Supported: use the matching `-KC` JAR |
| Older Keycloak versions | Not published or tested; you can try building it yourself (see below) |

The default build targets the Keycloak version in `pom.xml` (`keycloak.version`). CI builds every pull request against that version and against the oldest Keycloak version the next release will publish for.

## Building against another Keycloak version

```bash
mvn clean package -Dkeycloak.version=<keycloak-version>
```

The JAR is written to `target/keycloak-2fa-email-authenticator-v<plugin-version>.jar`.

## Requirements

- Java 21 to build the plugin. The tests may fail on Java 25 and newer, see the [troubleshooting guide](https://mesutpiskin.github.io/keycloak-2fa-email-authenticator/troubleshooting).
