package com.example.rewards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

  private static final Logger logger = LoggerFactory.getLogger(OpenApiConfig.class);

  @Value("${server.port:8080}")
  private String serverPort;

  // Support both APP_SERVER_URL (environment variable) and app.server.url (properties/Secret Manager)
  @Value("${app.server.url:${APP_SERVER_URL:}}")
  private String serverUrl;

  @Bean
  public OpenAPI customOpenAPI() {
    List<Server> servers = new ArrayList<>();
    
    // Log the server URL for debugging
    logger.info("OpenAPI Config - Server URL from config: {}", serverUrl);
    logger.info("OpenAPI Config - Server Port: {}", serverPort);
    
    // If server URL is explicitly configured (e.g., via environment variable or Secret Manager)
    if (serverUrl != null && !serverUrl.isEmpty() && !serverUrl.equals("http://localhost:8080")) {
      // Remove trailing slash if present
      String cleanUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
      servers.add(new Server()
        .url(cleanUrl)
        .description("Production server"));
      logger.info("OpenAPI Config - Added production server: {}", cleanUrl);
    } else {
      logger.warn("OpenAPI Config - No production server URL configured. Using localhost only.");
    }
    
    // Always add localhost for local development
    String localhostUrl = "http://localhost:" + serverPort;
    servers.add(new Server()
      .url(localhostUrl)
      .description("Local development server"));
    logger.info("OpenAPI Config - Added localhost server: {}", localhostUrl);
    
    return new OpenAPI()
      .info(new Info()
        .title("Rewards API")
        .version("1.0.0")
        .description("This is a sample Rewards Server based on the OpenAPI 3.0 specification. " +
          "You can find out more about Swagger at [https://swagger.io](https://swagger.io). " +
          "This API allows you to manage rewards for users with full CRUD operations.")
        .contact(new Contact()
          .name("API Support")
          .email("support@example.com")
          .url("https://www.example.com/support"))
        .license(new License()
          .name("Apache 2.0")
          .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
      .servers(servers);
  }
}
