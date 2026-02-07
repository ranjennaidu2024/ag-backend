package com.example.rewards.service;

import com.example.rewards.model.Project;
import com.example.rewards.repo.ProjectRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public Flux<Project> findAll() {
        return repository.findAll();
    }

    public Mono<Project> findById(String id) {
        return repository.findById(id);
    }

    public Mono<Project> create(Mono<Project> projectMono) {
        return projectMono.flatMap(repository::save);
    }

    public Mono<Project> update(String id, Mono<Project> projectMono) {
        return repository.findById(id)
                .zipWith(projectMono, (existing, incoming) -> {
                    existing.setName(incoming.getName());
                    existing.setStatus(incoming.getStatus());
                    existing.setType(incoming.getType());
                    existing.setProgress(incoming.getProgress());
                    return existing;
                })
                .flatMap(repository::save);
    }

    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }
}
