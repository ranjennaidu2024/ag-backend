# Upgrade to Java 25, Spring Boot 4.0.2, and OpenAPI 3.1.0

This document describes the upgrade performed on the antigravity-project backend and provides a testing checklist to ensure all functionality remains intact.

---

## Summary of Changes

### 1. Java Version
- **Before:** Java 21
- **After:** Java 25
- **Impact:** Enables Java 25 features (pattern matching, virtual threads, structured concurrency). Ensure your CI/CD and local development use JDK 25.

### 2. Spring Boot Version
- **Before:** 3.4.1
- **After:** 4.0.2
- **Impact:** Major version upgrade with modular architecture, Jackson 3, Spring Framework 7, Jakarta EE 11 baseline. See [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) for full details.

### 3. Swagger UI / OpenAPI
- **Before:** springdoc-openapi 2.6.0 with OpenAPI 3.0
- **After:** springdoc-openapi 3.0.1 with **OpenAPI 3.1.0**
- **Configuration:** Added `springdoc.api-docs.version: openapi_3_1` in `application.yml`
- **Impact:** OpenAPI 3.1.0 spec support (JSON Schema alignment, improved nullability). Swagger UI remains at `/swagger-ui.html`.

### 4. GCP Secret Manager
- **Before:** google-cloud-secretmanager 2.7.0
- **After:** google-cloud-secretmanager 2.38.0
- **Impact:** Updated native client for compatibility with Java 25. **No code changes** – `GcpSecretManagerConfig` and `ApplicationListener<ApplicationEnvironmentPreparedEvent>` remain unchanged and compatible.

### 5. Test Dependencies (Spring Boot 4 Modular Structure)
- **Before:** `spring-boot-starter-test` + `reactor-test`
- **After:** `spring-boot-starter-webflux-test` + `spring-boot-starter-data-mongodb-reactive-test`
- **Impact:** Aligns with Spring Boot 4’s modular test starters. Both bring `spring-boot-starter-test` transitively.
- **Fallback:** If you encounter dependency resolution issues, replace with `spring-boot-starter-test` and `spring-boot-starter-test-classic` per the [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).

### 6. Docker
- **Before:** `maven:3.9-eclipse-temurin-21`, `eclipse-temurin:21-jre-alpine`
- **After:** `maven:3-eclipse-temurin-25-alpine`, `eclipse-temurin:25-jre-alpine`
- **Impact:** Build and runtime images use Java 25.

---

## What Was NOT Changed (Preserved Behavior)

- **GCP Secret Manager integration** – Same `GcpSecretManagerConfig`, `spring.factories` registration, and secret loading flow
- **MongoDB reactive** – Same `spring-boot-starter-data-mongodb-reactive` usage
- **WebFlux routing** – Same `RewardRouter`, `ProjectRouter`, handlers, and functional endpoints
- **OpenAPI config** – Same `OpenApiConfig` (server URLs, `APP_SERVER_URL`, `app.server.url`)
- **Profiles** – Same `local`, `dev`, `qa`, `uat`, `prod` behavior
- **Actuator** – Same health/info endpoints

---

## Prerequisites for Testing

1. **JDK 25** – Install and set `JAVA_HOME`:
   ```bash
   # macOS with SDKMAN
   sdk install java 25-tem
   sdk use java 25-tem
   ```

2. **Maven 3.9+** – Ensure Maven uses JDK 25:
   ```bash
   mvn -version
   # Should show Java version 25
   ```

3. **MongoDB** – Local MongoDB for `local` profile (unchanged)

4. **GCP credentials** – For `dev`/`qa`/`uat`/`prod` profiles (unchanged)

---

## Testing Checklist

### Build & Compile
- [ ] `mvn clean compile` succeeds
- [ ] `mvn clean package -DskipTests` succeeds
- [ ] `mvn clean verify` (with tests) succeeds

### Local Profile (No GCP)
- [ ] Start MongoDB locally
- [ ] Run with `spring.profiles.active=local`
- [ ] App starts without errors
- [ ] Health: `GET http://localhost:8080/actuator/health` returns 200
- [ ] Swagger UI: `http://localhost:8080/swagger-ui.html` loads
- [ ] OpenAPI JSON: `http://localhost:8080/v3/api-docs` returns valid OpenAPI 3.1 spec
- [ ] Rewards CRUD: `GET/POST/PUT/DELETE /api/rewards` work
- [ ] Projects CRUD: `GET/POST/PUT/DELETE /api/projects` work

### GCP Profile (dev/qa/uat/prod)
- [ ] Set `GOOGLE_APPLICATION_CREDENTIALS` to a valid service account key
- [ ] Run with `spring.profiles.active=dev` (or qa/uat/prod)
- [ ] App starts and logs: `Successfully loaded N properties from GCP Secret Manager`
- [ ] MongoDB connects using URI from Secret Manager
- [ ] All API endpoints respond correctly
- [ ] Swagger UI shows correct server URL when `APP_SERVER_URL` or `app.server.url` is set

### OpenAPI 3.1.0 Verification
- [ ] `GET /v3/api-docs` returns JSON with `"openapi": "3.1.0"`
- [ ] Swagger UI renders all endpoints
- [ ] “Try it out” works for sample requests

### Docker Build
- [ ] `docker build -t antigravity-backend .` succeeds
- [ ] `docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local antigravity-backend` starts
- [ ] Health and API endpoints respond inside container

### Cloud Run Deployment (If Applicable)
- [ ] Cloud Build completes successfully
- [ ] Cloud Run service deploys and starts
- [ ] GCP Secret Manager secrets load for `prod` profile
- [ ] Swagger UI accessible at Cloud Run URL
- [ ] API requests succeed

---

## Considerations When Testing

1. **Jackson 3** – Spring Boot 4 uses Jackson 3. If you have custom serializers/deserializers, review [Jackson 3 migration](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0).

2. **JUnit** – Spring Boot 4 uses JUnit 6. Existing JUnit 5 tests should still run; if you see issues, check JUnit 6 migration notes.

3. **EnvironmentPostProcessor** – `GcpSecretManagerConfig` uses `ApplicationListener<ApplicationEnvironmentPreparedEvent>`, not `EnvironmentPostProcessor`, so no changes are required.

4. **Docker base images** – If `maven:3-eclipse-temurin-25-alpine` or `eclipse-temurin:25-jre-alpine` are unavailable in your registry, switch to `eclipse-temurin:25` or another supported Java 25 image.

5. **Cloud Build** – Ensure the Cloud Build environment uses a Java 25-capable image if building inside GCP.

---

## Cloud Run: MongoDB "Connection refused localhost:27017"

If you see `Connection refused: localhost/127.0.0.1:27017` on Cloud Run, the app is not getting the MongoDB URI. The app now supports **three** ways to provide it:

### Option A: Cloud Run "Reference a secret" (Recommended)

1. In GCP Secret Manager, create secret `webflux-mongodb-rest-prod` with value (properties format):
   ```properties
   spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
   app.environment=production
   ```

2. In Cloud Run → Variables & Secrets → **Secrets exposed as environment variables**:
   - Name: `backend-prod-secret` (auto-detected by the app)
   - Secret: `webflux-mongodb-rest-prod`
   - Version: `latest`

3. Set `SPRING_PROFILES_ACTIVE` = `prod` in Environment variables.

### Option B: Direct environment variable

Set in Cloud Run → Environment variables:
- Name: `SPRING_DATA_MONGODB_URI` (or `MONGODB_URI`)
- Value: `mongodb+srv://user:pass@cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority`

**Note:** If the password contains `$`, `#`, or other special characters, use Option A (secret) to avoid escaping issues.

### Option C: GCP Secret Manager API

The app loads from the API when `gcp.secretmanager.enabled=true` and profile is not `local`. Ensure:
- `SPRING_PROFILES_ACTIVE` = `prod`
- Secret `webflux-mongodb-rest-prod` exists with `spring.data.mongodb.uri`
- Cloud Run service account has `roles/secretmanager.secretAccessor`

---

## Rollback

If you need to revert:

1. Restore `pom.xml` from version control (Java 21, Spring Boot 3.4.1, springdoc 2.6.0, google-cloud-secretmanager 2.7.0).
2. Remove `springdoc.api-docs.version: openapi_3_1` from `application.yml`.
3. Restore `Dockerfile` to use Java 21 images.
4. Restore test dependencies to `spring-boot-starter-test` and `reactor-test`.

---

## File Changes Summary

| File | Changes |
|------|---------|
| `pom.xml` | Spring Boot 4.0.2, Java 25, springdoc 3.0.1, google-cloud-secretmanager 2.38.0, modular test starters |
| `application.yml` | Added `springdoc.api-docs.version: openapi_3_1` |
| `Dockerfile` | Java 25 base images |
| `UPGRADE_README.md` | New file – this document |

No Java source files were modified. GCP integration, routers, handlers, and configuration remain unchanged.
