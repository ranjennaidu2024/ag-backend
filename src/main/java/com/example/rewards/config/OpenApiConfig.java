package com.example.rewards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
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
      .servers(List.of(
        new Server()
          .url("http://localhost:8080")
          .description("Development server")
      ));
  }
}
