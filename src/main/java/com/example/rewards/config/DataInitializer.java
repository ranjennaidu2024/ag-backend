package com.example.rewards.config;

import com.example.rewards.model.Project;
import com.example.rewards.model.Reward;
import com.example.rewards.repo.ProjectRepository;
import com.example.rewards.repo.RewardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class DataInitializer {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
  private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(10);

  @Bean
  @Order(2) // Run after MongoConnectionValidator (Order 1)
  ApplicationRunner initData(ReactiveMongoTemplate template, RewardRepository rewardRepository, ProjectRepository projectRepository, Environment environment) {
    return args -> {
      // Check if we're in a cloud environment
      boolean isCloudEnvironment = isCloudEnvironment(environment);
      String mongoUri = environment.getProperty("spring.data.mongodb.uri");
      
      log.info("=".repeat(60));
      log.info("Database Initialization");
      log.info("Environment: {}", isCloudEnvironment ? "CLOUD" : "LOCAL");
      log.info("MongoDB URI: {}", mongoUri != null ? maskUri(mongoUri) : "NOT_CONFIGURED");
      log.info("=".repeat(60));
      
      // CRITICAL: For cloud environments, validate MongoDB URI before proceeding
      if (isCloudEnvironment) {
        validateCloudUri(mongoUri);
        log.info("✓ Cloud environment: MongoDB URI validated (points to remote database)");
      }
      
      // Proceed with initialization
      log.info("Starting database initialization...");
      
      Mono<Void> initRewards = ensureCollection(template, Reward.class)
        .then(rewardRepository.count())
        .timeout(OPERATION_TIMEOUT)
        .filter(count -> count == 0)
        .flatMapMany(ignored -> {
          log.info("Rewards Database is empty. Seeding with sample data...");
          return rewardRepository.saveAll(sampleRewards());
        })
        .then();

      Mono<Void> initProjects = ensureCollection(template, Project.class)
        .then(projectRepository.count())
        .timeout(OPERATION_TIMEOUT)
        .filter(count -> count == 0)
        .flatMapMany(ignored -> {
           log.info("Projects Database is empty. Seeding with sample data...");
           return projectRepository.saveAll(sampleProjects());
        })
        .then();

      Mono.when(initRewards, initProjects)
        .doOnSuccess(ignored -> {
           log.info("✅ Database initialization completed successfully.");
           log.info("=".repeat(60));
        })
        .doOnError(e -> {
           log.error("❌ CRITICAL: Database initialization FAILED!", e);
           log.error("This indicates a MongoDB connection issue. Application cannot continue.");
        })
        .onErrorResume(e -> Mono.error(new IllegalStateException("Failed to initialize database.", e)))
        .subscribe(
            null,
            error -> {
                log.error("Fatal error during database initialization. Application will terminate.", error);
                System.exit(1);
            }
        );
    };
  }
  
  private void validateCloudUri(String mongoUri) {
      if (mongoUri == null || mongoUri.isEmpty()) {
          String errorMsg = "❌ CRITICAL: MongoDB URI is NOT configured in CLOUD environment!";
          log.error(errorMsg);
          throw new IllegalStateException(errorMsg);
      }
      if (mongoUri.contains("localhost") || mongoUri.contains("127.0.0.1")) {
          String errorMsg = "❌ CRITICAL: MongoDB URI points to LOCALHOST in CLOUD environment!";
          log.error(errorMsg);
          throw new IllegalStateException(errorMsg);
      }
  }

  private boolean isCloudEnvironment(Environment environment) {
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("local".equals(profile)) {
        return false;
      }
    }
    return true;
  }
  
  private String maskUri(String uri) {
    if (uri == null || uri.isEmpty()) {
      return "NOT_CONFIGURED";
    }
    return uri.replaceAll("://([^:]+):([^@]+)@", "://$1:***@");
  }

  private Mono<Void> ensureCollection(ReactiveMongoTemplate template, Class<?> entityClass) {
    return template.collectionExists(entityClass)
      .timeout(OPERATION_TIMEOUT)
      .flatMap(exists -> {
        if (exists) {
          log.info("Collection '{}' already exists.", entityClass.getSimpleName());
          return Mono.empty();
        } else {
          log.info("Creating collection '{}'...", entityClass.getSimpleName());
          return template.createCollection(entityClass).then();
        }
      });
  }

  private Flux<Reward> sampleRewards() {
    return Flux.just(
      new Reward("user-1", 100, "welcome bonus"),
      new Reward("user-2", 250, "referral bonus"),
      new Reward("user-3", 75, "feedback reward")
    );
  }

  private Flux<Project> sampleProjects() {
      return Flux.just(
          new Project("Project Alpha", "Running", "Web App", 75),
          new Project("Project Beta", "Ended", "Mobile App", 100),
          new Project("Project Gamma", "Pending", "Desktop App", 0),
          new Project("Project Delta", "Running", "Web App", 25)
      );
  }
}
