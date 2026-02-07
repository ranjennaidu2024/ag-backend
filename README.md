# WebFlux MongoDB Rewards API

Reactive Spring Boot WebFlux application (Boot 3.4.1) with MongoDB, OpenAPI, and GCP Secret Manager integration.

## Prerequisites

- JDK 21 or higher
- Maven 3.9+
- MongoDB (for local development)
- Google Cloud Project with billing enabled (for cloud environments)

---

## Environment Profiles

The application supports multiple environment profiles:

| Profile | Configuration Source | GCP Required | MongoDB |
|---------|---------------------|--------------|---------|
| `local` | Direct YAML configuration | ❌ No | localhost:27017 |
| `dev` | GCP Secret Manager | ✅ Yes | Cloud/Atlas |
| `qa` | GCP Secret Manager | ✅ Yes | Cloud/Atlas |
| `uat` | GCP Secret Manager | ✅ Yes | Cloud/Atlas |
| `prod` | GCP Secret Manager | ✅ Yes | Cloud/Atlas |

---

## Quick Start

### Local Development (No GCP Required)

1. **Start MongoDB locally:**
   ```bash
   # macOS with Homebrew
   brew services start mongodb-community
   
   # Or with Docker
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   ```

2. **Update active profile in `application.yml`:**
   ```yaml
   spring:
     profiles:
       active: local
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the API:**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Base: http://localhost:8080/api/rewards

---

## GCP Secret Manager Setup (For Cloud Environments)

For cloud environments (dev, qa, uat, prod), the application loads configuration from GCP Secret Manager using the native GCP Secret Manager client.

### How It Works

- Secrets are loaded programmatically via `GcpSecretManagerConfig`
- Secret names follow the pattern: `webflux-mongodb-rest-{profile}`
- Example: Profile `dev` → Secret: `webflux-mongodb-rest-dev`
- All configuration (including MongoDB URI) is stored in GCP Secret Manager
- Secrets are parsed as properties format and loaded into Spring environment

### Step 1: Enable Secret Manager API

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Search for **"Secret Manager API"**
4. Click **"Enable"**

### Step 2: Create Secret in GCP Console

1. Navigate to **Security** → **Secret Manager**
2. Click **"+ CREATE SECRET"**
3. Configure the secret:
   - **Name**: `webflux-mongodb-rest-dev` (must match exactly: `webflux-mongodb-rest-{profile}`)
   - **Secret value**: Paste your configuration in properties format (see example below)
   - **Regions**: Leave as "Automatic" (default)

4. **Example Secret Value (Properties Format):**
   ```properties
   spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
   app.environment=development
   app.name=rewards-api-dev
   api.external.url=https://api-dev.example.com
   api.external.key=dev-api-key-12345
   ```

5. Click **"CREATE SECRET"**

**Important Notes:**
- Secret name must match exactly: `webflux-mongodb-rest-{profile}`
- Use properties format (one `key=value` per line)
- All sensitive configuration should be in the secret, not in YAML files
- Create separate secrets for each environment (dev, qa, uat, prod)

### Step 3: Create Service Account

1. Navigate to **IAM & Admin** → **Service Accounts**
2. Click **"Create Service Account"**
3. **Service account details:**
   - **Name**: `rewards-app-sa`
   - **Description**: `Service account for Rewards Application to access Secret Manager`
   - Click **"Create and Continue"**

4. **Grant access:**
   - **Role**: Select **"Secret Manager Secret Accessor"**
   - Click **"Continue"** → **"Done"**

### Step 4: Download Service Account Key

1. Click on the service account email
2. Go to **"Keys"** tab
3. Click **"Add Key"** → **"Create new key"**
4. Select **JSON** format
5. Click **"Create"** (file will download automatically)
6. Save the file securely (e.g., `~/rewards-app-key.json`)

**Security:**
- Never commit this key file to version control
- Add `*.json` to `.gitignore`
- Restrict file permissions: `chmod 600 ~/rewards-app-key.json`

### Step 5: Set Environment Variable

**macOS/Linux:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/rewards-app-key.json"

# Make it permanent
echo 'export GOOGLE_APPLICATION_CREDENTIALS="$HOME/rewards-app-key.json"' >> ~/.zshrc
source ~/.zshrc
```

**Windows (PowerShell):**
```powershell
[System.Environment]::SetEnvironmentVariable(
    'GOOGLE_APPLICATION_CREDENTIALS',
    "$env:USERPROFILE\rewards-app-key.json",
    'User'
)
```

**Verify:**
```bash
echo $GOOGLE_APPLICATION_CREDENTIALS
# Should show the path to your key file
```

### Step 6: Configure Application

1. **Update `application.yml` with your GCP project ID:**
   ```yaml
   gcp:
     secretmanager:
       enabled: true
       project-id: your-gcp-project-id
   ```

2. **Set active profile:**
   ```yaml
   spring:
     profiles:
       active: dev  # Change to: dev, qa, uat, or prod
   ```

### Step 7: Run the Application

```bash
mvn spring-boot:run
```

The application will:
1. Load secrets from GCP Secret Manager based on active profile
2. Connect to MongoDB using URI from the secret
3. Start on http://localhost:8080

---

## Configuration Files

### Base Configuration (`application.yml`)
- Contains common settings for all profiles
- GCP Secret Manager enabled by default
- Project ID configured here

### Profile-Specific Files

**`application-local.yml`**
- Disables GCP Secret Manager
- Uses local MongoDB: `mongodb://localhost:27017/rewardsdb`

**`application-dev.yml`**
- Inherits base GCP Secret Manager config
- Adds debug logging for Secret Manager

**`application-qa.yml`, `application-uat.yml`, `application-prod.yml`**
- Inherit base GCP Secret Manager config
- Add debug logging for Secret Manager

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/rewards` | List all rewards |
| `GET` | `/api/rewards/{id}` | Get reward by ID |
| `GET` | `/api/rewards/user/{userId}` | Get rewards for a user |
| `POST` | `/api/rewards` | Create new reward |
| `PUT` | `/api/rewards/{id}` | Update reward |
| `DELETE` | `/api/rewards/{id}` | Delete reward |

**Example Request Body (POST/PUT):**
```json
{
  "userId": "user-123",
  "points": 150,
  "description": "Completed tutorial"
}
```

---

## API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

---

## Switching Between Environments

**Option 1: Update `application.yml`**
```yaml
spring:
  profiles:
    active: qa  # Change to: dev, qa, uat, or prod
```

**Option 2: Command line override**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=qa
```

---

## Secret Templates

### Development (`webflux-mongodb-rest-dev`)
```properties
spring.data.mongodb.uri=mongodb+srv://devuser:password@dev-cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
app.environment=development
app.name=rewards-api-dev
api.external.url=https://api-dev.example.com
api.external.key=dev-api-key-12345
logging.level.com.example.rewards=DEBUG
```

### QA (`webflux-mongodb-rest-qa`)
```properties
spring.data.mongodb.uri=mongodb+srv://qauser:password@qa-cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
app.environment=qa
app.name=rewards-api-qa
api.external.url=https://api-qa.example.com
api.external.key=qa-api-key-67890
logging.level.com.example.rewards=INFO
```

### UAT (`webflux-mongodb-rest-uat`)
```properties
spring.data.mongodb.uri=mongodb+srv://uatuser:password@uat-cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
app.environment=uat
app.name=rewards-api-uat
api.external.url=https://api-uat.example.com
api.external.key=uat-api-key-abcde
logging.level.com.example.rewards=INFO
```

### Production (`webflux-mongodb-rest-prod`)
```properties
spring.data.mongodb.uri=mongodb+srv://produser:password@prod-cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
app.environment=production
app.name=rewards-api-prod
app.version=1.0.0
api.external.url=https://api.example.com
api.external.key=prod-SECURE-API-KEY
logging.level.com.example.rewards=WARN
spring.data.mongodb.auto-index-creation=false
```

**Replace:**
- `username/password` → Your actual MongoDB credentials
- `cluster.mongodb.net` → Your MongoDB cluster URL
- `api-key-*` → Your actual API keys

---

## Troubleshooting

### Local Profile Issues

**Application won't start:**
- Ensure MongoDB is running: `mongosh mongodb://localhost:27017`
- Verify `application.yml` has `active: local`
- Check `application-local.yml` has correct MongoDB URI

### Cloud Profile Issues (dev/qa/uat/prod)

**Application won't start:**
- Verify `GOOGLE_APPLICATION_CREDENTIALS` is set: `echo $GOOGLE_APPLICATION_CREDENTIALS`
- Check service account key file exists and is valid JSON
- Ensure Secret Manager API is enabled in GCP
- Verify service account has "Secret Manager Secret Accessor" role

**Can't connect to MongoDB:**
- Verify secret exists in GCP Secret Manager with correct name: `webflux-mongodb-rest-{profile}`
- Check secret contains `spring.data.mongodb.uri` property
- Verify MongoDB URI is correct in the secret
- Ensure MongoDB URI does NOT point to localhost for cloud environments
- Test MongoDB connection string manually

**Secret Manager errors:**
- Verify secret names match exactly: `webflux-mongodb-rest-{profile}`
- Check active profile matches secret name (e.g., profile `dev` needs secret `webflux-mongodb-rest-dev`)
- Ensure service account has "Secret Manager Secret Accessor" role
- Verify Secret Manager API is enabled
- Check project ID in `application.yml` matches your GCP project

**Common Error Messages:**

1. **"Secret not found"**
   - Verify secret exists in GCP Secret Manager console
   - Check secret name matches exactly: `webflux-mongodb-rest-{profile}`
   - Ensure active profile matches secret name

2. **"Permission denied"**
   - Verify service account has "Secret Manager Secret Accessor" role
   - Check `GOOGLE_APPLICATION_CREDENTIALS` points to valid key file
   - Ensure key file has correct permissions

3. **"Could not resolve placeholder 'spring.data.mongodb.uri'"**
   - Verify secret contains `spring.data.mongodb.uri` property
   - Check secret format is correct (properties format)
   - Ensure secret was created successfully in GCP Console

---

## Project Structure

```
src/main/java/com/example/rewards/
├── api/                    # REST handlers and router
│   ├── GlobalErrorHandler.java
│   ├── RewardHandler.java
│   └── RewardRouter.java
├── config/                 # Configuration classes
│   ├── DataInitializer.java
│   ├── GcpSecretManagerConfig.java  # GCP Secret Manager integration
│   ├── MongoConnectionValidator.java
│   ├── OpenApiConfig.java
│   └── WebConfig.java
├── model/                  # Domain models
│   └── Reward.java
├── repo/                   # MongoDB repository
│   └── RewardRepository.java
├── service/                # Business logic
│   └── RewardService.java
└── WebfluxMongodbRestApplication.java
```

---

## Technology Stack

- **Spring Boot**: 3.4.1
- **Java**: 21
- **Spring WebFlux**: Reactive web framework
- **MongoDB Reactive**: Reactive MongoDB driver
- **GCP Secret Manager**: Native client (2.7.0)
- **SpringDoc OpenAPI**: 2.6.0 (Swagger UI)
- **Maven**: Build tool

---

## Additional Documentation

See [SWAGGER_SETUP.md](SWAGGER_SETUP.md) for detailed Swagger/OpenAPI configuration information.


test3