package com.example.rewards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

  @Value("${server.port:8080}")
  private String serverPort;

  @Value("${app.server.url:}")
  private String serverUrl;

  @Bean
  public OpenAPI customOpenAPI() {
    List<Server> servers = new ArrayList<>();
    
    // If server URL is explicitly configured (e.g., via environment variable or Secret Manager)
    if (serverUrl != null && !serverUrl.isEmpty() && !serverUrl.equals("http://localhost:8080")) {
      servers.add(new Server()
        .url(serverUrl)
        .description("Production server"));
    }
    
    // Always add localhost for local development
    servers.add(new Server()
      .url("http://localhost:" + serverPort)
      .description("Local development server"));
    
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
