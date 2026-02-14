# Upgrade Guide: Java 25, Spring Boot 3.5.10, OpenAPI 3.1.0

This document describes the upgrade performed on the antigravity-project backend and provides a testing checklist to ensure all functionality remains intact.

---

## Summary of Changes

### 1. Java Version
| Before | After |
|--------|-------|
| Java 21 | **Java 25** |

- Updated `java.version`, `maven.compiler.source`, and `maven.compiler.target` in `pom.xml`
- Updated Dockerfile build and runtime images to use Eclipse Temurin 25

**Prerequisite:** Ensure JDK 25 is installed locally for development:
```bash
java -version
# Should show: openjdk version "25" or similar
```

### 2. Spring Boot Version
| Before | After |
|--------|-------|
| 3.4.1 | **3.5.10** |

- Spring Boot 3.5.10 includes bug fixes, dependency upgrades, and improved Java 25 support
- All Spring Boot starters (webflux, validation, data-mongodb-reactive, actuator) are managed by the parent POM and updated automatically

### 3. Swagger UI & OpenAPI
| Component | Before | After |
|-----------|--------|-------|
| springdoc-openapi-starter-webflux-ui | 2.6.0 | **2.8.15** |
| OpenAPI Specification | 3.0 (default) | **3.1.0** |

- **springdoc-openapi 2.8.15** – Latest version compatible with Spring Boot 3.5.x; includes Swagger UI 5.31.0 and swagger-core 2.2.41
- **OpenAPI 3.1.0** – Enabled via `springdoc.api-docs.version: openapi_3_1` in `application.yml`
- OpenAPI 3.1.0 adds JSON Schema 2020-12 support, improved `oneOf`/`anyOf` handling, and webhook support

### 4. GCP Secret Manager
| Before | After |
|--------|-------|
| google-cloud-secretmanager 2.7.0 | **2.80.0** |

- Updated to latest version with Java 25 compatibility
- **No code changes** – `GcpSecretManagerConfig` and integration remain unchanged
- API usage is backward compatible

### 5. Docker
| Stage | Before | After |
|-------|--------|-------|
| Build | maven:3.9-eclipse-temurin-21 | **maven:3.9-eclipse-temurin-25** |
| Runtime | eclipse-temurin:21-jre-alpine | **eclipse-temurin:25-jre-alpine** |

---

## Files Modified

| File | Changes |
|------|---------|
| `pom.xml` | Spring Boot 3.5.10, Java 25, springdoc 2.8.15, google-cloud-secretmanager 2.80.0 |
| `src/main/resources/application.yml` | Added `springdoc.api-docs.version: openapi_3_1` |
| `src/main/java/com/example/rewards/config/OpenApiConfig.java` | Updated description text to "OpenAPI 3.1.0" |
| `Dockerfile` | Java 25 base images for build and runtime |

---

## What Was NOT Changed (Preserved)

- **GCP Secret Manager integration** – `GcpSecretManagerConfig` unchanged; secrets still loaded from `webflux-mongodb-rest-{profile}`
- **MongoDB Reactive** – Same configuration and repository usage
- **API endpoints** – Rewards and Projects routers, handlers, services unchanged
- **Environment profiles** – local, dev, qa, uat, prod behavior unchanged
- **Actuator** – Health and info endpoints unchanged
- **Cloud Run deployment** – `cloudbuild.yaml` unchanged; compatible with new image

---

## Testing Checklist

### Prerequisites
- [ ] JDK 25 installed (`java -version`)
- [ ] Maven 3.9+ (`mvn -version`)
- [ ] MongoDB running (for local profile)
- [ ] GCP credentials configured (for dev/qa/uat/prod profiles)

### 1. Build Verification
```bash
cd antigravity-project/backend
mvn clean package -DskipTests
```
- [ ] Build completes successfully
- [ ] No dependency resolution errors
- [ ] JAR created in `target/`

### 2. Local Profile (No GCP)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
- [ ] Application starts without errors
- [ ] MongoDB connection established
- [ ] Health check: `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- [ ] Swagger UI loads: http://localhost:8080/swagger-ui.html
- [ ] OpenAPI JSON: http://localhost:8080/v3/api-docs – verify `"openapi": "3.1.0"` in response
- [ ] Test Rewards API: `GET /api/rewards`, `POST /api/rewards`
- [ ] Test Projects API: `GET /api/projects`, `POST /api/projects`

### 3. GCP Secret Manager (dev/qa/uat/prod)
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account-key.json"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- [ ] Application starts and loads secrets from GCP
- [ ] Log shows: `Successfully loaded N properties from GCP Secret Manager`
- [ ] MongoDB connects using URI from secret
- [ ] Swagger UI and API endpoints work as in local profile

### 4. Docker Build & Run
```bash
docker build -t antigravity-backend:test .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local antigravity-backend:test
```
- [ ] Docker build completes (may need `--platform linux/amd64` on Apple Silicon)
- [ ] Container starts and health check passes
- [ ] API and Swagger UI accessible

### 5. Cloud Run Deployment (if applicable)
- [ ] Push to repository triggers Cloud Build
- [ ] Build succeeds with Java 25 base image
- [ ] Deploy new revision to Cloud Run
- [ ] Service starts; check logs for GCP Secret Manager success
- [ ] Swagger UI shows correct server URL (if `APP_SERVER_URL` or `app.server.url` configured)
- [ ] All API endpoints respond correctly

### 6. OpenAPI 3.1.0 Verification
- [ ] Visit http://localhost:8080/v3/api-docs
- [ ] Response includes `"openapi": "3.1.0"`
- [ ] Swagger UI renders all endpoints correctly
- [ ] "Try it out" works for POST/PUT requests

---

## Considerations & Known Items

### Java 25
- Java 25 is GA (September 2025). Ensure your CI/CD and deployment environments support it.
- If Cloud Build or other pipelines use a fixed Java version, update them to Java 25.

### Maven Docker Image
- If `maven:3.9-eclipse-temurin-25` is not available in your registry, try:
  - `maven:3-eclipse-temurin-25`
  - `maven:3-eclipse-temurin-25-alpine`

### OpenAPI 3.1.0
- Some older API clients or code generators may expect OpenAPI 3.0. If you encounter compatibility issues, you can revert to 3.0 by removing or changing:
  ```yaml
  springdoc:
    api-docs:
      version: openapi_3_1  # Remove this line to use default 3.0
  ```

### GCP Secret Manager
- No changes to secret structure or naming. Existing secrets (`webflux-mongodb-rest-dev`, etc.) work as before.
- Service account permissions unchanged.

---

## Rollback (If Needed)

To rollback to the previous versions, revert the following in `pom.xml`:
- Spring Boot: `3.4.1`
- Java: `21`
- springdoc-openapi-starter-webflux-ui: `2.6.0`
- google-cloud-secretmanager: `2.7.0`

And in `Dockerfile`:
- Build: `maven:3.9-eclipse-temurin-21`
- Runtime: `eclipse-temurin:21-jre-alpine`

Remove `springdoc.api-docs.version: openapi_3_1` from `application.yml` if you want OpenAPI 3.0.
