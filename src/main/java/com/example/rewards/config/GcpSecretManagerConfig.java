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
import java.util.Properties;

/**
 * Configuration class to load secrets from GCP Secret Manager.
 * This replaces the Spring Cloud GCP Secret Manager auto-configuration
 * with a more explicit and recommended approach using the native GCP client.
 *
 * Uses Ordered.LOWEST_PRECEDENCE to run after config files are loaded, ensuring
 * spring.profiles.active and active profiles are available.
 */
public class GcpSecretManagerConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GcpSecretManagerConfig.class);
    private static final String SECRET_NAME_PREFIX = "webflux-mongodb-rest-";
    private static final String GCP_PROJECT_ID_PROPERTY = "gcp.secretmanager.project-id";
    private static final String GCP_SECRET_ENABLED_PROPERTY = "gcp.secretmanager.enabled";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        // Check if GCP Secret Manager is enabled
        boolean enabled = environment.getProperty(GCP_SECRET_ENABLED_PROPERTY, Boolean.class, false);
        if (!enabled) {
            logger.debug("GCP Secret Manager is disabled. Skipping secret loading.");
            return;
        }

        // Get active profile - with fallback for timing/ordering (run after config loaders)
        String activeProfile = resolveActiveProfile(environment);
        if (activeProfile == null || activeProfile.isEmpty()) {
            logger.debug("No active profile found. Skipping GCP Secret Manager.");
            return;
        }
        
        // Skip local profile - it doesn't use GCP
        if ("local".equals(activeProfile)) {
            logger.debug("Local profile detected. Skipping GCP Secret Manager.");
            return;
        }

        logger.debug("Using profile for GCP Secret Manager: {}", activeProfile);

        // Get GCP project ID
        String projectId = environment.getProperty(GCP_PROJECT_ID_PROPERTY);
        if (projectId == null || projectId.isEmpty()) {
            logger.warn("GCP project ID not configured. Set '{}' property to enable Secret Manager.", 
                    GCP_PROJECT_ID_PROPERTY);
            return;
        }

        // Construct secret name based on profile
        String secretName = SECRET_NAME_PREFIX + activeProfile;
        
        try {
            logger.info("Loading secrets from GCP Secret Manager: project={}, secret={}", 
                    projectId, secretName);
            
            Properties secrets = loadSecretsFromGcp(projectId, secretName);
            
            if (secrets != null && !secrets.isEmpty()) {
                PropertySource<?> propertySource = new PropertiesPropertySource(
                        "gcp-secret-manager", secrets);
                environment.getPropertySources().addFirst(propertySource);
                logger.info("Successfully loaded {} properties from GCP Secret Manager", 
                        secrets.size());
                if (!secrets.containsKey("spring.data.mongodb.uri")) {
                    logger.warn("Secret '{}' does not contain 'spring.data.mongodb.uri'. " +
                            "MongoDB will use default (localhost:27017). Add the URI to the secret.", secretName);
                }
            } else {
                logger.warn("No secrets found or secret is empty: {}", secretName);
            }
        } catch (Exception e) {
            logger.error("Failed to load secrets from GCP Secret Manager: {}", 
                    e.getMessage(), e);
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

