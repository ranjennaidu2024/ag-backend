package com.example.rewards.config;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Configuration class to load secrets from GCP Secret Manager.
 * Supports:
 * 1. GCP Secret Manager API (secret: webflux-mongodb-rest-{profile})
 * 2. Cloud Run "Reference a secret" - when secret is exposed as env var (properties format)
 * 3. Direct env vars: SPRING_DATA_MONGODB_URI or MONGODB_URI
 *
 * Uses Ordered.LOWEST_PRECEDENCE to run after config files are loaded.
 */
public class GcpSecretManagerConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GcpSecretManagerConfig.class);
    private static final String SECRET_NAME_PREFIX = "webflux-mongodb-rest-";
    private static final String GCP_PROJECT_ID_PROPERTY = "gcp.secretmanager.project-id";
    private static final String GCP_SECRET_ENABLED_PROPERTY = "gcp.secretmanager.enabled";
    private static final String CLOUDRUN_SECRET_ENV_VAR_PROPERTY = "gcp.secretmanager.cloudrun-secret-env-var";
    private static final String MONGODB_URI_PROPERTY = "spring.data.mongodb.uri";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Properties loadedSecrets = new Properties();

        // 1. Cloud Run: Check for secret exposed as env var (e.g., backend-prod-secret)
        String cloudRunEnvVar = environment.getProperty(CLOUDRUN_SECRET_ENV_VAR_PROPERTY);
        if (cloudRunEnvVar == null || cloudRunEnvVar.isEmpty()) {
            cloudRunEnvVar = System.getenv("GCP_CLOUDRUN_SECRET_ENV_VAR");
        }
        if (cloudRunEnvVar == null || cloudRunEnvVar.isEmpty()) {
            // Default: check common Cloud Run secret env var names (backend-{profile}-secret)
            String profile = resolveActiveProfile(environment);
            String[] namesToCheck = profile != null
                    ? new String[]{"backend-" + profile + "-secret", "backend-prod-secret"}
                    : new String[]{"backend-prod-secret"};
            for (String name : namesToCheck) {
                if (System.getenv(name) != null) {
                    cloudRunEnvVar = name;
                    break;
                }
            }
        }
        if (cloudRunEnvVar != null && !cloudRunEnvVar.isEmpty()) {
            String secretValue = System.getenv(cloudRunEnvVar);
            if (secretValue != null && !secretValue.isEmpty()) {
                try {
                    Properties cloudRunProps = new Properties();
                    cloudRunProps.load(new StringReader(secretValue));
                    loadedSecrets.putAll(cloudRunProps);
                    logger.info("Loaded {} properties from Cloud Run secret env var: {}", 
                            cloudRunProps.size(), cloudRunEnvVar);
                } catch (Exception e) {
                    logger.warn("Failed to parse Cloud Run secret env var '{}': {}", cloudRunEnvVar, e.getMessage());
                }
            }
        }

        // 2. GCP Secret Manager API (when enabled and not local profile)
        if (loadedSecrets.isEmpty()) {
            Properties apiSecrets = loadFromGcpApi(environment);
            if (apiSecrets != null) {
                loadedSecrets.putAll(apiSecrets);
            }
        }

        // 3. Fallback: Direct env vars (Cloud Run allows dots in env var names)
        if (!loadedSecrets.containsKey(MONGODB_URI_PROPERTY)) {
            String mongoUri = System.getenv("SPRING_DATA_MONGODB_URI");
            if (mongoUri == null || mongoUri.isEmpty()) {
                mongoUri = System.getenv("MONGODB_URI");
            }
            if (mongoUri == null || mongoUri.isEmpty()) {
                mongoUri = System.getenv("spring.data.mongodb.uri");
            }
            if (mongoUri != null && !mongoUri.isEmpty()) {
                loadedSecrets.setProperty(MONGODB_URI_PROPERTY, mongoUri);
                logger.info("Using MongoDB URI from env var (SPRING_DATA_MONGODB_URI or MONGODB_URI)");
            }
        }

        if (!loadedSecrets.isEmpty()) {
            PropertySource<?> propertySource = new PropertiesPropertySource(
                    "gcp-secret-manager", loadedSecrets);
            environment.getPropertySources().addFirst(propertySource);
            logger.info("Successfully loaded {} properties into environment", loadedSecrets.size());
            if (!loadedSecrets.containsKey(MONGODB_URI_PROPERTY)) {
                logger.warn("Loaded config does not contain 'spring.data.mongodb.uri'. " +
                        "MongoDB will use default (localhost:27017). Add the URI to the secret or set SPRING_DATA_MONGODB_URI env var.");
            }
        }
    }

    /**
     * Load secrets from GCP Secret Manager API.
     */
    private Properties loadFromGcpApi(ConfigurableEnvironment environment) {
        boolean enabled = environment.getProperty(GCP_SECRET_ENABLED_PROPERTY, Boolean.class, false);
        if (!enabled) {
            logger.debug("GCP Secret Manager is disabled. Skipping API load.");
            return null;
        }

        String activeProfile = resolveActiveProfile(environment);
        if (activeProfile == null || activeProfile.isEmpty()) {
            logger.debug("No active profile found. Skipping GCP Secret Manager API.");
            return null;
        }
        if ("local".equals(activeProfile)) {
            logger.debug("Local profile detected. Skipping GCP Secret Manager API.");
            return null;
        }

        String projectId = environment.getProperty(GCP_PROJECT_ID_PROPERTY);
        if (projectId == null || projectId.isEmpty()) {
            projectId = System.getenv("GCP_SECRETMANAGER_PROJECT_ID");
        }
        if (projectId == null || projectId.isEmpty()) {
            projectId = System.getenv("GCP_PROJECT_ID");
        }
        if (projectId == null || projectId.isEmpty()) {
            logger.warn("GCP project ID not configured. Set gcp.secretmanager.project-id or GCP_PROJECT_ID env var.");
            return null;
        }

        String secretName = SECRET_NAME_PREFIX + activeProfile;
        try {
            logger.info("Loading secrets from GCP Secret Manager API: project={}, secret={}", projectId, secretName);
            return loadSecretsFromGcp(projectId, secretName);
        } catch (Exception e) {
            logger.error("Failed to load secrets from GCP Secret Manager API: {}", e.getMessage());
            throw new RuntimeException("Failed to load secrets from GCP Secret Manager", e);
        }
    }

    /**
     * Loads secrets from GCP Secret Manager.
     * 
     * @param projectId GCP project ID
     * @param secretName Name of the secret in Secret Manager
     * @return Properties object containing the secrets
     * @throws IOException if there's an error accessing Secret Manager
     */
    private Properties loadSecretsFromGcp(String projectId, String secretName) throws IOException {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            // Access the latest version of the secret
            SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretName, "latest");
            
            logger.debug("Accessing secret version: {}", secretVersionName);
            
            // Get the secret payload
            com.google.cloud.secretmanager.v1.AccessSecretVersionResponse response = 
                    client.accessSecretVersion(secretVersionName);
            
            byte[] secretData = response.getPayload().getData().toByteArray();
            
            if (secretData.length == 0) {
                logger.warn("Secret '{}' is empty", secretName);
                return new Properties();
            }

            // Parse the secret content as properties
            Properties properties = new Properties();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(secretData)) {
                properties.load(inputStream);
                logger.debug("Parsed {} properties from secret", properties.size());
            }

            return properties;
        } catch (com.google.api.gax.rpc.NotFoundException e) {
            logger.error("Secret '{}' not found in project '{}'. " +
                    "Please create the secret in GCP Secret Manager.", secretName, projectId);
            throw new RuntimeException("Secret not found: " + secretName, e);
        } catch (com.google.api.gax.rpc.PermissionDeniedException e) {
            logger.error("Permission denied accessing secret '{}' in project '{}'. " +
                    "Please check your GCP credentials and IAM permissions.", secretName, projectId);
            throw new RuntimeException("Permission denied accessing secret: " + secretName, e);
        }
    }

    /**
     * Resolves the active profile, with fallbacks for listener ordering.
     * Runs with LOWEST_PRECEDENCE so config files (and spring.profiles.active) are loaded first.
     */
    private String resolveActiveProfile(ConfigurableEnvironment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles != null && activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        // Fallback: read spring.profiles.active directly (env var SPRING_PROFILES_ACTIVE or property)
        String profile = environment.getProperty("spring.profiles.active");
        if (profile != null && !profile.isEmpty()) {
            return profile.split(",")[0].trim();
        }
        return null;
    }
}

