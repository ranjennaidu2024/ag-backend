# WebFlux MongoDB Rewards API

Reactive Spring Boot WebFlux application (Boot 4.0.2) with MongoDB, OpenAPI 3.1.0, and GCP Secret Manager integration.

## Prerequisites

- JDK 25 or higher
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
app.server.url=https://antigravity-backend-xxxxx-uc.a.run.app
api.external.url=https://api.example.com
api.external.key=prod-SECURE-API-KEY
logging.level.com.example.rewards=WARN
spring.data.mongodb.auto-index-creation=false
```

**Note:** `app.server.url` is used by Swagger UI to generate correct API request URLs. Replace with your actual Cloud Run service URL.

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

- **Spring Boot**: 4.0.2
- **Java**: 25
- **Spring WebFlux**: Reactive web framework
- **MongoDB Reactive**: Reactive MongoDB driver
- **GCP Secret Manager**: Native client (2.38.0)
- **SpringDoc OpenAPI**: 3.0.1 (Swagger UI, OpenAPI 3.1.0)
- **Maven**: Build tool

> **Upgrade notes:** See [UPGRADE_README.md](UPGRADE_README.md) for the upgrade from Java 21 / Spring Boot 3.4.1 and testing checklist.

---

## Deploying to Google Cloud Platform (GCP)

This guide will walk you through deploying the backend application to Google Cloud Platform using **Cloud Run** via the GCP Console UI only (no CLI required).

### Prerequisites

1. A Google Cloud Platform account ([Sign up here](https://cloud.google.com/) if you don't have one)
2. A GCP project created (or you'll create one during deployment)
3. A credit card (GCP offers free credits for new users)
4. MongoDB Atlas account (for cloud MongoDB) or MongoDB instance accessible from GCP
5. Your GCP Project ID (you'll need this for configuration)

### Step-by-Step Deployment Guide

#### Step 1: Create a GCP Project (if not already created)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click on the project dropdown at the top of the page (next to "Google Cloud")
3. Click **"NEW PROJECT"**
4. Enter a project name (e.g., `antigravity-backend`)
5. Click **"CREATE"**
6. Wait for the project to be created, then select it from the project dropdown
7. **Note your Project ID** (displayed in the project dropdown) - you'll need this later

#### Step 2: Enable Required APIs

1. In the GCP Console, click the **☰ (hamburger menu)** in the top left
2. Navigate to **"APIs & Services"** > **"Library"**
3. Enable the following APIs (search for each and click **"ENABLE"**):
   - **Cloud Run API**
   - **Cloud Build API**
   - **Container Registry API**
   - **Secret Manager API** (required for configuration)
   - **Artifact Registry API** (if using Artifact Registry)

#### Step 3: Set Up MongoDB (MongoDB Atlas Recommended)

**Option A: MongoDB Atlas (Recommended for Production)**

1. Go to [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Sign up or log in
3. Create a new cluster (free tier available)
4. Create a database user:
   - Go to **"Database Access"** > **"Add New Database User"**
   - Choose **"Password"** authentication
   - Create username and password (save these securely)
   - Set privileges to **"Atlas admin"** or **"Read and write to any database"**
5. Configure network access:
   - Go to **"Network Access"** > **"Add IP Address"**
   - Click **"Allow Access from Anywhere"** (or add specific GCP IP ranges)
   - Click **"Confirm"**
6. Get your connection string:
   - Go to **"Database"** > Click **"Connect"** on your cluster
   - Choose **"Connect your application"**
   - Copy the connection string (format: `mongodb+srv://username:password@cluster.mongodb.net/dbname?retryWrites=true&w=majority`)
   - **Save this connection string** - you'll need it for Secret Manager

**Option B: Use Existing MongoDB Instance**

- Ensure your MongoDB instance is accessible from GCP Cloud Run
- Note the connection string format: `mongodb://host:port/database` or `mongodb+srv://...`

#### Step 4: Create GCP Secret Manager Secret

The application uses GCP Secret Manager to store configuration. You need to create a secret for the production profile.

1. In GCP Console, navigate to **"Security"** > **"Secret Manager"**
2. Click **"+ CREATE SECRET"**
3. Configure the secret:
   - **Name**: `webflux-mongodb-rest-prod` (must match exactly: `webflux-mongodb-rest-{profile}`)
   - **Secret value**: Paste your configuration in properties format (see example below)
   - **Regions**: Leave as "Automatic" (default)

4. **Example Secret Value (Properties Format):**
   ```properties
   spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/rewardsdb?retryWrites=true&w=majority
   app.environment=production
   app.name=antigravity-backend-prod
   logging.level.com.example.rewards=INFO
   ```
   
   **Important:** Replace:
   - `username:password` → Your MongoDB Atlas credentials
   - `cluster.mongodb.net` → Your MongoDB Atlas cluster URL
   - `rewardsdb` → Your database name

5. Click **"CREATE SECRET"**

#### Step 5: Update Project ID in Configuration

Before deploying, you need to update the GCP project ID in your configuration:

1. Open `src/main/resources/application.yml`
2. Find the `gcp.secretmanager.project-id` property
3. Replace `moonlit-botany-480813-p3` (or the existing value) with your actual GCP project ID
4. Save the file

**Example:**
```yaml
gcp:
  secretmanager:
    enabled: true
    project-id: your-actual-project-id  # Replace this
```

**Note:** You can also set this via environment variable `GCP_SECRETMANAGER_PROJECT_ID` in Cloud Run, but updating the file is simpler and ensures consistency.

#### Step 6: Prepare Your Code for Upload

You have two options:

**Option A: Upload via Cloud Source Repositories (Recommended)**
1. In GCP Console, click **☰** menu > **"Source Repositories"**
2. Click **"CREATE REPOSITORY"**
3. Select **"Create new repository"**
4. Name it `antigravity-backend` and click **"CREATE"**
5. Follow the instructions to push your code (you can use the provided git commands, or upload manually via the web interface)

**Option B: Connect GitHub Repository**
1. In GCP Console, go to **"Cloud Build"** > **"Triggers"**
2. When creating a trigger, you can connect your GitHub account
3. Select your repository and branch

#### Step 7: Create a Cloud Build Trigger

**Important:** This step uses an **Inline YAML configuration** instead of the "Dockerfile" option to avoid UI bugs and properly handle logging requirements.

1. In GCP Console, click **☰** menu > **"Cloud Build"** > **"Triggers"**
2. Click **"CREATE TRIGGER"**
3. Configure the trigger:
   - **Name**: `antigravity-backend-build`
   - **Event**: Select **"Push to a branch"**
   - **Source**: 
     - If using Source Repositories: Select your repository
     - If using GitHub: Connect your GitHub account and select the repository
   - **Branch**: `^main$` (or your main branch name, e.g., `^master$`)
   
4. **Configuration Settings (Critical):**
   - **Configuration**: Select **"Cloud Build configuration file (yaml or json)"** (NOT "Dockerfile")
   - **Location**: Select **"Inline"** (this allows you to paste the YAML directly)
   - **Cloud Build configuration file contents**: Paste the following YAML:
   
   ```yaml
   steps:
     # Build the image using the project ID variable
     - name: 'gcr.io/cloud-builders/docker'
       args: [
         'build', 
         '-t', 'gcr.io/$PROJECT_ID/backend:$COMMIT_SHA', 
         '.'
       ]
   
     # Push the image to the registry
     - name: 'gcr.io/cloud-builders/docker'
       args: ['push', 'gcr.io/$PROJECT_ID/backend:$COMMIT_SHA']
   
   options:
     # This line solved the "Failed to trigger build" error
     logging: CLOUD_LOGGING_ONLY
   ```

5. **Service Account Configuration (if using custom service account):**
   - Scroll down to **"Advanced"** section
   - If you're using a custom service account, you'll need to grant it proper permissions (see Step 6a below)
   - If using default service account, you can skip to Step 7

6. Click **"CREATE"**

#### Step 7a: Configure Service Account Permissions (Required if using custom service account)

If you're using a custom service account for Cloud Build, you need to grant it the following IAM roles:

1. Go to **"IAM & Admin"** > **"IAM"** in GCP Console
2. Find your service account (or the Cloud Build service account)
3. Click the **✏️ (edit/pencil icon)** next to the service account
4. Click **"ADD ANOTHER ROLE"** and add these roles:
   - **Logs Writer** (`roles/logging.logWriter`) - To send logs to Cloud Logging
   - **Artifact Registry Writer** (`roles/artifactregistry.writer`) - To upload the image
   - **Artifact Registry Create-on-push Writer** (`roles/artifactregistry.createOnPushWriter`) - To allow automatic creation of the "backend" repository during first push
   - **Secret Manager Secret Accessor** (`roles/secretmanager.secretAccessor`) - To read secrets from Secret Manager (required for the application to run)
5. Click **"SAVE"**

**Note:** If you're using the default compute service account, these permissions are usually already configured. The custom service account approach gives you more control but requires manual permission setup.

#### Step 8: Build Your Container Image

**If using Source Repositories:**
1. Push your code to the repository (or use the web interface to upload files)
2. Go to **"Cloud Build"** > **"History"**
3. Your build should start automatically
4. Wait for the build to complete (you'll see a green checkmark when done)

**If using GitHub:**
1. Push your code to the connected GitHub repository
2. Go to **"Cloud Build"** > **"History"**
3. Your build should start automatically
4. Wait for the build to complete

**If uploading manually:**
1. Go to **"Cloud Build"** > **"History"**
2. Click **"RUN TRIGGER"** or **"RUN"** button
3. Select your trigger
4. Click **"RUN"**
5. Wait for the build to complete

#### Step 9: Deploy to Cloud Run

1. In GCP Console, click **☰** menu > **"Cloud Run"**
2. Click **"CREATE SERVICE"**
3. Configure your service:

   **Service Settings:**
   - **Service name**: `antigravity-backend`
   - **Region**: Choose a region close to your users (e.g., `us-central1`, `us-east1`, `europe-west1`)
   - **Deploy one revision from an existing container image**: Click **"SELECT"**
   - **Container image URL**: Click **"SELECT"** and choose the image you just built (it should be named something like `gcr.io/YOUR-PROJECT-ID/backend:COMMIT_SHA`)
     - **Note:** The image name format is `gcr.io/YOUR-PROJECT-ID/backend:COMMIT_SHA` based on the Cloud Build configuration
     - You can select any commit SHA tag, or use the latest build
   - **Container port**: `8080`
   - **CPU allocation**: Select **"CPU is only allocated during request processing"** (to save costs)
   - **Memory**: `512 MiB` (minimum, increase to `1 GiB` or `2 GiB` if needed for MongoDB operations)
   - **Minimum number of instances**: `0` (to allow scaling to zero)
   - **Maximum number of instances**: `10` (adjust as needed)
   - **Concurrency**: `80` (default)
   - **Request timeout**: `300` seconds

   **Container Settings:**
   - Click **"Container"** tab
   - **Environment variables**: Add the following:
     - `SPRING_PROFILES_ACTIVE` = `prod`
     - `GCP_SECRETMANAGER_PROJECT_ID` = `YOUR-PROJECT-ID` (replace with your actual GCP project ID)
       - **Note:** This overrides the `gcp.secretmanager.project-id` value in `application.yml`
       - **Alternative:** You can also update `application.yml` directly with your project ID before building
   - **Port**: `8080`

   **Security:**
   - **Authentication**: Select **"Allow unauthenticated invocations"** (to make it publicly accessible)
   - **Service account**: Select the service account that has **"Secret Manager Secret Accessor"** role (or use default compute service account)

4. Click **"CREATE"** or **"DEPLOY"**
5. Wait for the deployment to complete (this may take a few minutes)

#### Step 10: Configure Swagger Server URL (Important for Swagger UI)

**IMPORTANT:** You need to rebuild and redeploy the backend with the updated code first, then set the environment variable.

**Step 10a: Rebuild Backend with Updated Code**

The `OpenApiConfig.java` has been updated to support dynamic server URLs. You need to rebuild:

1. **Push the updated code** to your repository (the updated `OpenApiConfig.java` file)
2. **Trigger a new build:**
   - Go to **"Cloud Build"** > **"History"**
   - Click **"RUN TRIGGER"** (or push a commit to trigger automatic build)
   - Select your backend trigger
   - Wait for build to complete
3. **Redeploy with new image:**
   - Go to **"Cloud Run"** > Your backend service
   - Click **"EDIT & DEPLOY NEW REVISION"**
   - Select the newly built image
   - Click **"DEPLOY"**

**Step 10b: Set Environment Variable**

After redeploying with the updated code, set the server URL:

1. Go to **"Cloud Run"** > Select your backend service
2. Click **"EDIT & DEPLOY NEW REVISION"**
3. Scroll to **"Container"** section → **"Environment variables"**
4. Add or update:
   - **Name**: `APP_SERVER_URL`
   - **Value**: `https://your-backend-cloud-run-url`
     - Replace with your actual Cloud Run service URL (the one shown at the top of the service page)
     - Example: `https://antigravity-backend-xxxxx-uc.a.run.app`
     - **Important:** Use `https://` and don't include `/api` or trailing slashes
5. Click **"DEPLOY"** to apply the changes
6. After deployment, Swagger UI will show your Cloud Run URL in the server dropdown

**Alternative: Set via Secret Manager**
You can also add `app.server.url=https://your-backend-url` to your GCP Secret Manager secret (`webflux-mongodb-rest-prod`), and it will be loaded automatically. Then restart the Cloud Run service.

**Verify it's working:**
- Check Cloud Run logs - you should see: `OpenAPI Config - Server URL from config: https://your-url`
- Open Swagger UI - the server dropdown should show your Cloud Run URL first

#### Step 11: Grant Cloud Run Service Account Access to Secret Manager

The Cloud Run service needs permission to read secrets from Secret Manager:

1. Go to **"IAM & Admin"** > **"IAM"** in GCP Console
2. Find the Cloud Run service account (format: `PROJECT_NUMBER-compute@developer.gserviceaccount.com`)
   - You can find your PROJECT_NUMBER in **"IAM & Admin"** > **"Settings"**
3. Click the **✏️ (edit/pencil icon)** next to the service account
4. Click **"ADD ANOTHER ROLE"**
5. Add role: **Secret Manager Secret Accessor** (`roles/secretmanager.secretAccessor`)
6. Click **"SAVE"**

#### Step 12: Access Your Deployed Application

1. Once deployment is complete, you'll see your service in the Cloud Run dashboard
2. Click on your service name (`antigravity-backend`)
3. You'll see a **URL** at the top of the page (e.g., `https://antigravity-backend-xxxxx-uc.a.run.app`)
4. Test your endpoints:
   - **Health Check**: `https://your-service-url/actuator/health`
   - **Swagger UI**: `https://your-service-url/swagger-ui.html`
   - **API Base**: `https://your-service-url/api/projects`
5. Your application is now publicly accessible!

#### Step 13: Testing Swagger UI

Swagger UI provides an interactive interface to test your API endpoints directly from the browser. Here's how to access and use it:

**1. Access Swagger UI:**
   - Open your browser and navigate to: `https://your-service-url/swagger-ui.html`
   - Example: `https://antigravity-backend-xxxxx-uc.a.run.app/swagger-ui.html`
   - You should see the Swagger UI interface with all available API endpoints
   - **Check the server dropdown** at the top - it should show your Cloud Run URL, not `localhost:8080`
   - If you see `localhost:8080`, make sure `APP_SERVER_URL` environment variable is set (see Step 10)

**2. Verify Swagger UI is Loading:**
   - If you see a page with API documentation, Swagger UI is working correctly
   - The page should display:
     - API title and description
     - List of available endpoints grouped by tags
     - Request/response schemas
     - "Try it out" buttons for each endpoint

**3. Test API Endpoints:**

   The application provides the following endpoints that you can test in Swagger UI:

   **Available Endpoints:**
   - `GET /api/projects` - Get all projects
   - `GET /api/projects/{id}` - Get project by ID
   - `POST /api/projects` - Create a new project
   - `PUT /api/projects/{id}` - Update a project
   - `DELETE /api/projects/{id}` - Delete a project
   - `GET /actuator/health` - Health check endpoint

   **Example 1: Test GET /api/projects (Get All Projects)**
   1. In Swagger UI, find the **"Projects"** section
   2. Expand the **GET /api/projects** endpoint
   3. Click the **"Try it out"** button
   4. Click **"Execute"** button
   5. Review the response:
      - **Response Code**: Should be `200` for successful requests
      - **Response Body**: Should show JSON array with project information
      - **Response Headers**: Shows content-type (`application/json`) and other headers

### Managing Your Deployment

#### Viewing Logs
1. Go to **"Cloud Run"** > Select your service
2. Click on **"LOGS"** tab to view application logs
3. You can filter and search logs here
4. Logs will show Secret Manager loading, MongoDB connections, and API requests

#### Updating Your Application
1. Make changes to your code
2. Push to your repository (or upload new files)
3. Cloud Build will automatically trigger (if configured) or manually run the build
4. Once the new image is built, go to **"Cloud Run"** > Your service > **"EDIT & DEPLOY NEW REVISION"**
5. Select the new container image
6. Click **"DEPLOY"**

#### Updating Secrets
1. Go to **"Secret Manager"** > Select your secret (`webflux-mongodb-rest-prod`)
2. Click **"ADD NEW VERSION"**
3. Enter the new secret value
4. Click **"ADD VERSION"**
5. The application will automatically use the latest version on next request (or restart the Cloud Run service to force reload)

#### Setting Custom Domain (Optional)
1. Go to **"Cloud Run"** > Your service > **"MANAGE CUSTOM DOMAINS"**
2. Click **"ADD MAPPING"**
3. Follow the instructions to verify domain ownership
4. Configure DNS settings as instructed

### Cost Considerations

- **Cloud Run** charges based on:
  - CPU and memory usage during request processing
  - Number of requests
  - Network egress
- With the configuration above (scaling to zero), you'll only pay when your app receives traffic
- GCP offers a **Free Tier** with generous limits for Cloud Run
- **MongoDB Atlas** offers a free tier (M0 cluster) for development
- Estimated cost for low-traffic sites: **$0-10/month** (often free within free tier limits)

### Troubleshooting

#### Build Fails
- Check **"Cloud Build"** > **"History"** for error details
- Ensure `Dockerfile` is in the root of your project
- Verify `pom.xml` has all required dependencies
- Check that Java 25 is specified in Dockerfile

#### Application Not Starting
- Check **"Cloud Run"** > **"LOGS"** for error messages
- Verify environment variables are set correctly:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `GCP_SECRETMANAGER_PROJECT_ID` matches your project ID (or check `application.yml` has correct `gcp.secretmanager.project-id`)
- Ensure the Cloud Run service account has **Secret Manager Secret Accessor** role
- Verify the secret `webflux-mongodb-rest-prod` exists in Secret Manager

#### Swagger UI Still Shows localhost:8080

**CRITICAL:** Make sure you've rebuilt and redeployed with the updated `OpenApiConfig.java` code first!

- **Step 1: Verify code is updated**
  1. Check that `OpenApiConfig.java` has the updated code (should read `APP_SERVER_URL` environment variable)
  2. If not, push the updated code and rebuild (see Step 10a above)

- **Step 2: Check environment variable:**
  1. Go to **"Cloud Run"** > Your backend service > **"EDIT & DEPLOY NEW REVISION"**
  2. Check **"Environment variables"** section
  3. Ensure `APP_SERVER_URL` is set to your Cloud Run URL (e.g., `https://antigravity-backend-xxxxx-uc.a.run.app`)
  4. **Important:** Use `https://` protocol and don't include `/api` or trailing slashes
  5. Click **"DEPLOY"** to apply changes

- **Step 3: Check logs:**
  1. Go to **"Cloud Run"** > Your backend service > **"LOGS"** tab
  2. Look for log lines starting with `OpenAPI Config -`
  3. You should see: `OpenAPI Config - Server URL from config: https://your-url`
  4. If it shows empty or localhost, the environment variable isn't being read

- **Step 4: Verify in Swagger UI:**
  - Open Swagger UI: `https://your-backend-url/swagger-ui.html`
  - Look at the **server dropdown** at the top of the page
  - It should show your Cloud Run URL first, then localhost
  - If it still shows only localhost:
    - Check logs (Step 3) to see what URL was detected
    - Verify environment variable is set correctly (Step 2)
    - Make sure you rebuilt with the updated code (Step 1)

- **Alternative: Set via Secret Manager**
  - Add `app.server.url=https://your-backend-url` to your Secret Manager secret (`webflux-mongodb-rest-prod`)
  - Restart the Cloud Run service to reload secrets
  - Check logs to verify it's being read

#### Can't Connect to MongoDB
- Check **"Cloud Run"** > **"LOGS"** for MongoDB connection errors
- Verify MongoDB Atlas network access allows connections from GCP (or "Allow from anywhere")
- Ensure the MongoDB URI in Secret Manager is correct
- Test MongoDB connection string manually from your local machine
- Verify MongoDB credentials are correct in the secret

#### Secret Manager Errors
- Verify secret exists: `webflux-mongodb-rest-prod`
- Check Cloud Run service account has `roles/secretmanager.secretAccessor` role
- Ensure Secret Manager API is enabled
- Verify project ID in environment variables matches your GCP project
- Check logs for specific Secret Manager error messages

#### Application Not Accessible
- Check that **"Allow unauthenticated invocations"** is enabled
- Verify the service is deployed and running (green status)
- Check the **"LOGS"** tab for runtime errors
- Verify the health endpoint: `/actuator/health`

#### Container Image Not Found
- Ensure Cloud Build completed successfully
- Check **"Container Registry"** > **"Images"** to verify image exists
- Verify you're selecting the correct image in Cloud Run deployment
- Image should be named: `gcr.io/YOUR-PROJECT-ID/backend:COMMIT_SHA`

#### Cloud Build Trigger Error: "Failed to trigger build: if 'build.service_account' is specified..."

This error occurs when a service account is specified but logging configuration is missing. The recommended solution is to use the **Inline YAML configuration** approach described in Step 6.

**Solution 1: Use Inline YAML Configuration (Recommended - Works Every Time)**
1. Go to **"Cloud Build"** > **"Triggers"**
2. Click on your trigger name (`antigravity-backend-build`)
3. Click **"EDIT"** button
4. Change **"Configuration"** from **"Dockerfile"** to **"Cloud Build configuration file (yaml or json)"**
5. Set **"Location"** to **"Inline"**
6. Paste the YAML configuration from Step 6 (with `logging: CLOUD_LOGGING_ONLY` in options)
7. Click **"SAVE"**

**Solution 2: Fix Service Account Permissions**
If you're still getting "Denied" or "Repo Not Found" errors:
1. Follow **Step 7a** above to grant proper IAM roles to your service account
2. Ensure these roles are added:
   - `roles/logging.logWriter`
   - `roles/artifactregistry.writer`
   - `roles/artifactregistry.createOnPushWriter`
   - `roles/secretmanager.secretAccessor`

**Why Inline YAML Works Better:**
- The "Dockerfile" configuration option sometimes hides logging settings in the UI
- Inline YAML gives you full control over all build options
- The `logging: CLOUD_LOGGING_ONLY` option in YAML explicitly satisfies the logging requirement

### Cloud Build Configuration File

This project includes a `cloudbuild.yaml` file that can be used for Cloud Build deployments. The file is configured to:
- Build the Docker image using the project ID and commit SHA as tags
- Push the image to Google Container Registry (GCR)
- Use Cloud Logging only (no custom logs bucket required)

You can use this file directly in Cloud Build triggers, or use the inline YAML configuration as described in the deployment steps above.

---

## Additional Documentation

See [SWAGGER_SETUP.md](SWAGGER_SETUP.md) for detailed Swagger/OpenAPI configuration information.

```

IMPORTANT NOTE: ALWAYS MAKE SURE LATEST CONTAINER IMAGE IS SELECTED WHEN DEPLOY VIA CLOUD RUN