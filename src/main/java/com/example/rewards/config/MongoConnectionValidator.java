package com.example.rewards.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Validates MongoDB connection on application startup for all environments.
 * For local: Allows localhost connections
 * For non-local (dev/qa/uat/prod): Enforces remote MongoDB and fails fast if connection issues
 * 
 * Runs BEFORE DataInitializer to ensure validation passes before any database operations.
 */
@Component
@Order(1) // Run before DataInitializer (Order 2)
public class MongoConnectionValidator implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger log = LoggerFactory.getLogger(MongoConnectionValidator.class);
  private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);

  private final ReactiveMongoTemplate mongoTemplate;
  private final Environment environment;

  public MongoConnectionValidator(
      ReactiveMongoTemplate mongoTemplate,
      Environment environment) {
    this.mongoTemplate = mongoTemplate;
    this.environment = environment;
  }

  private boolean isLocalProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("local".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    String[] activeProfiles = environment.getActiveProfiles();
    boolean isLocal = isLocalProfile();
    
    log.info("=".repeat(60));
    log.info("MongoDB Connection Validation");
    log.info("Active profiles: {}", String.join(", ", activeProfiles));
    log.info("Environment type: {}", isLocal ? "LOCAL" : "CLOUD");
    log.info("=".repeat(60));
    
    // Get MongoDB URI from environment (after all properties are loaded)
    String mongoUri = environment.getProperty("spring.data.mongodb.uri");
    log.info("MongoDB URI loaded: {}", mongoUri != null ? maskSensitiveInfo(mongoUri) : "NOT_CONFIGURED");
    
    if (isLocal) {
      // Local profile: Allow localhost, but still validate connection
      validateLocalEnvironment(mongoUri);
    } else {
      // Non-local profiles (dev/qa/uat/prod): Enforce remote MongoDB
      validateCloudEnvironment(mongoUri, activeProfiles);
    }
    
    // Validate actual connection to MongoDB
    validateConnection(mongoUri);
    
    log.info("✅ MongoDB connection validation SUCCESSFUL for {} environment", isLocal ? "LOCAL" : "CLOUD");
    log.info("=".repeat(60));
  }

  private void validateLocalEnvironment(String mongoUri) {
    log.info("Validating LOCAL environment...");
    
    if (mongoUri == null || mongoUri.isEmpty()) {
      String errorMsg = 
          "❌ MongoDB connection validation FAILED!\n" +
          "Local profile requires MongoDB URI to be configured.\n" +
          "Expected: mongodb://localhost:27017/rewardsdb in application-local.yml\n" +
          "Please verify:\n" +
          "  1. application-local.yml exists\n" +
          "  2. It contains: spring.data.mongodb.uri=mongodb://localhost:27017/rewardsdb\n" +
          "  3. MongoDB is running locally: brew services start mongodb-community";
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }
    
    log.info("✓ Local MongoDB URI configured: {}", maskSensitiveInfo(mongoUri));
  }

  private void validateCloudEnvironment(String mongoUri, String[] activeProfiles) {
    log.info("Validating CLOUD environment (dev/qa/uat/prod)...");
    
    // Debug: Check if Secret Manager is enabled
    Boolean smEnabled = environment.getProperty("spring.cloud.gcp.secretmanager.enabled", Boolean.class);
    log.info("Secret Manager enabled: {}", smEnabled);
    
    // Debug: Check GOOGLE_APPLICATION_CREDENTIALS
    String gcpCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    log.info("GOOGLE_APPLICATION_CREDENTIALS: {}", gcpCreds != null ? gcpCreds : "NOT SET");
    
    // Debug: Show all property sources
    if (environment instanceof ConfigurableEnvironment) {
      log.info("Property sources loaded:");
      ((ConfigurableEnvironment) environment).getPropertySources().forEach(ps -> {
        log.info("  - {}", ps.getName());
        // Check if Secret Manager property source is loaded
        if (ps.getName().contains("Secret") || ps.getName().contains("secret")) {
          log.info("    ✓ Secret Manager property source found!");
        }
      });
    }
    
    // Try to get any property starting with spring.data to debug
    log.info("Checking for any spring.data.mongodb properties...");
    try {
      if (environment instanceof ConfigurableEnvironment) {
        ConfigurableEnvironment configEnv = (ConfigurableEnvironment) environment;
        configEnv.getPropertySources().forEach(ps -> {
          if (ps instanceof org.springframework.core.env.EnumerablePropertySource) {
            org.springframework.core.env.EnumerablePropertySource<?> eps = 
                (org.springframework.core.env.EnumerablePropertySource<?>) ps;
            for (String propName : eps.getPropertyNames()) {
              if (propName.startsWith("spring.data.mongodb") || propName.startsWith("spring.cloud.gcp")) {
                log.info("  Found property: {} = {}", propName, 
                    propName.contains("uri") ? maskSensitiveInfo(String.valueOf(eps.getProperty(propName))) : eps.getProperty(propName));
              }
            }
          }
        });
      }
    } catch (Exception e) {
      log.warn("Could not enumerate properties: {}", e.getMessage());
    }
    
    // Check if MongoDB URI is configured
    if (mongoUri == null || mongoUri.isEmpty()) {
      String errorMsg = String.format(
          "❌ MongoDB connection validation FAILED!\n" +
          "Reason: MongoDB URI is NOT configured for cloud environment.\n" +
          "Current URI: NOT_CONFIGURED\n" +
          "Expected: Remote MongoDB URI from GCP Secret Manager.\n" +
          "\n" +
          "DEBUG INFO:\n" +
          "  - Active profiles: %s\n" +
          "  - Secret Manager enabled: %s\n" +
          "  - GOOGLE_APPLICATION_CREDENTIALS: %s\n" +
          "\n" +
          "Please verify:\n" +
          "  1. GCP Secret Manager contains 'spring.data.mongodb.uri' property\n" +
          "  2. GOOGLE_APPLICATION_CREDENTIALS environment variable is set\n" +
          "  3. Service account has 'Secret Manager Secret Accessor' role\n" +
          "  4. Secret name matches the active profile (e.g., webflux-mongodb-rest-dev)\n" +
          "  5. Secret Manager API is enabled in GCP\n" +
          "  6. Run with debug: mvn spring-boot:run -Dspring-boot.run.arguments=--debug",
          String.join(", ", activeProfiles),
          smEnabled,
          gcpCreds != null ? "SET" : "NOT SET"
      );
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }
    
    // Check if MongoDB URI points to localhost (not allowed in cloud environments)
    if (mongoUri.contains("localhost") || mongoUri.contains("127.0.0.1")) {
      String errorMsg = String.format(
          "❌ MongoDB connection validation FAILED!\n" +
          "Reason: MongoDB URI points to LOCALHOST in cloud environment.\n" +
          "Current URI: %s\n" +
          "Cloud environments (dev/qa/uat/prod) must use REMOTE MongoDB instances.\n" +
          "\n" +
          "Please update the secret in GCP Secret Manager with a remote MongoDB URI:\n" +
          "  - MongoDB Atlas: mongodb+srv://user:pass@cluster.mongodb.net/db\n" +
          "  - Cloud MongoDB: mongodb://host:port/db",
          maskSensitiveInfo(mongoUri)
      );
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }
    
    log.info("✓ Cloud MongoDB URI configured (remote): {}", maskSensitiveInfo(mongoUri));
  }

  private void validateConnection(String mongoUri) {
    log.info("Testing actual MongoDB connection...");
    
    try {
      mongoTemplate.getMongoDatabase()
          .flatMap(db -> Mono.from(db.listCollectionNames().first())
              .timeout(CONNECTION_TIMEOUT)
              .onErrorResume(e -> Mono.error(new RuntimeException("Failed to connect to MongoDB", e))))
          .doOnSuccess(name -> log.info("✓ Successfully connected to MongoDB database"))
          .doOnError(e -> {
            String errorMsg = String.format(
                "❌ MongoDB connection test FAILED!\n" +
                "Reason: Cannot establish connection to configured MongoDB instance.\n" +
                "URI: %s\n" +
                "Error: %s\n" +
                "Please verify:\n" +
                "  1. MongoDB instance is running and accessible\n" +
                "  2. Network connectivity to MongoDB cluster\n" +
                "  3. MongoDB credentials are valid\n" +
                "  4. For Atlas: IP whitelist includes your application's IP\n" +
                "  5. For local: MongoDB service is running (brew services start mongodb-community)",
                maskSensitiveInfo(mongoUri),
                e.getMessage()
            );
            log.error(errorMsg, e);
          })
          .block(); // Block to ensure validation happens before app fully starts
      
    } catch (Exception e) {
      String errorMsg = String.format(
          "❌ CRITICAL: MongoDB connection test FAILED!\n" +
          "The application cannot start because MongoDB connection could not be established.\n" +
          "URI: %s\n" +
          "Error: %s\n" +
          "\nTroubleshooting steps:\n" +
          "  1. Verify MongoDB instance is running\n" +
          "  2. Check network connectivity\n" +
          "  3. Verify MongoDB credentials\n" +
          "  4. Check firewall/IP whitelist settings\n" +
          "  5. Test connection manually with mongosh or MongoDB Compass",
          maskSensitiveInfo(mongoUri),
          e.getMessage()
      );
      log.error(errorMsg);
      throw new IllegalStateException(errorMsg, e);
    }
  }

  /**
   * Masks sensitive information (password) from MongoDB URI for logging
   */
  private String maskSensitiveInfo(String uri) {
    if (uri == null || uri.isEmpty()) {
      return "NOT_CONFIGURED";
    }
    // Mask password in connection string (e.g., mongodb+srv://user:password@host -> mongodb+srv://user:***@host)
    return uri.replaceAll("://([^:]+):([^@]+)@", "://$1:***@");
  }
}
