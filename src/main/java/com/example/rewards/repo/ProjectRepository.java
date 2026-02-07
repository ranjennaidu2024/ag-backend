package com.example.rewards.repo;

import com.example.rewards.model.Project;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends ReactiveMongoRepository<Project, String> {
}
