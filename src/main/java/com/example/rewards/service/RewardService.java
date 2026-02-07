package com.example.rewards.service;

import com.example.rewards.model.Reward;
import com.example.rewards.repo.RewardRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RewardService {
  private final RewardRepository repository;

  public RewardService(RewardRepository repository) {
    this.repository = repository;
  }

  public Flux<Reward> findAll() {
    return repository.findAll();
  }

  public Mono<Reward> findById(String id) {
    return repository.findById(id);
  }

  public Flux<Reward> findByUser(String userId) {
    return repository.findByUserId(userId);
  }

  public Mono<Reward> create(Mono<Reward> rewardMono) {
    return rewardMono.flatMap(repository::save);
  }

  public Mono<Reward> update(String id, Mono<Reward> rewardMono) {
    return repository.findById(id)
      .zipWith(rewardMono, (existing, incoming) -> {
        existing.setUserId(incoming.getUserId());
        existing.setPoints(incoming.getPoints());
        existing.setDescription(incoming.getDescription());
        return existing;
      })
      .flatMap(repository::save);
  }

  public Mono<Void> delete(String id) {
    return repository.deleteById(id);
  }
}

