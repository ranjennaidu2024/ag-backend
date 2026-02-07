package com.example.rewards.repo;

import com.example.rewards.model.Reward;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RewardRepository extends ReactiveMongoRepository<Reward, String> {
  Flux<Reward> findByUserId(String userId);
}

