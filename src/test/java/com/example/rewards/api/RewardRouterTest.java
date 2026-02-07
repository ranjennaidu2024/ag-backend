package com.example.rewards.api;

import com.example.rewards.model.Reward;
import com.example.rewards.repo.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardRouterTest {

  @Mock
  private RewardRepository repository;

  private WebTestClient client;

  private Reward reward;

  @BeforeEach
  void setup() {
    reward = new Reward("1", "user-1", 100, "welcome bonus");
    var service = new com.example.rewards.service.RewardService(repository);
    var handler = new RewardHandler(service, jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator());
    RouterFunction<ServerResponse> routes = new RewardRouter().routes(handler);
    client = WebTestClient.bindToRouterFunction(routes).build();
  }

  @Test
  void getAll() {
    when(repository.findAll()).thenReturn(Flux.just(reward));
    client.get()
      .uri("/api/rewards")
      .exchange()
      .expectStatus().isOk()
      .expectBodyList(Reward.class)
      .hasSize(1);
  }

  @Test
  void create() {
    when(repository.save(any(Reward.class))).thenReturn(Mono.just(reward));
    client.post()
      .uri("/api/rewards")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(reward)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.id").isEqualTo("1")
      .jsonPath("$.userId").isEqualTo("user-1");
  }
}

