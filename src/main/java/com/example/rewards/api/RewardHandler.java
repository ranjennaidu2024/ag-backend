package com.example.rewards.api;

import com.example.rewards.model.Reward;
import com.example.rewards.service.RewardService;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RewardHandler {

  private final RewardService service;
  private final SpringValidatorAdapter validator;

  public RewardHandler(RewardService service, Validator validator) {
    this.service = service;
    this.validator = new SpringValidatorAdapter(validator);
  }

  public Mono<ServerResponse> getAll(ServerRequest request) {
    return ServerResponse.ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(service.findAll(), Reward.class);
  }

  public Mono<ServerResponse> getById(ServerRequest request) {
    return service.findById(request.pathVariable("id"))
      .flatMap(reward -> ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(reward))
      .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> getByUser(ServerRequest request) {
    String userId = request.pathVariable("userId");
    return ServerResponse.ok()
      .contentType(MediaType.APPLICATION_JSON)
      .body(service.findByUser(userId), Reward.class);
  }

  public Mono<ServerResponse> create(ServerRequest request) {
    Mono<Reward> validated = request.bodyToMono(Reward.class).flatMap(this::validate);
    return service.create(validated)
      .flatMap(saved -> ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(saved));
  }

  public Mono<ServerResponse> update(ServerRequest request) {
    String id = request.pathVariable("id");
    Mono<Reward> validated = request.bodyToMono(Reward.class).flatMap(this::validate);
    return service.update(id, validated)
      .flatMap(saved -> ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(saved))
      .switchIfEmpty(ServerResponse.notFound().build());
  }

  public Mono<ServerResponse> delete(ServerRequest request) {
    return service.delete(request.pathVariable("id"))
      .then(ServerResponse.noContent().build());
  }

  private Mono<Reward> validate(Reward reward) {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(reward, Reward.class.getName());
    validator.validate(reward, errors);
    if (errors.hasErrors()) {
      return Mono.error(new IllegalArgumentException(errors.toString()));
    }
    return Mono.just(reward);
  }
}

