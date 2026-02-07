package com.example.rewards.api;

import com.example.rewards.model.Project;
import com.example.rewards.service.ProjectService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ProjectHandler {

    private final ProjectService service;

    public ProjectHandler(ProjectService service) {
        this.service = service;
    }

    public Mono<ServerResponse> getAllProjects(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAll(), Project.class);
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        return service.findById(request.pathVariable("id"))
                .flatMap(project -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(project))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        Mono<Project> projectMono = request.bodyToMono(Project.class);
        return service.create(projectMono)
                .flatMap(saved -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(saved));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Project> projectMono = request.bodyToMono(Project.class);
        return service.update(id, projectMono)
                .flatMap(saved -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(saved))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        return service.delete(request.pathVariable("id"))
                .then(ServerResponse.noContent().build());
    }
}
