package com.example.rewards.api;

import com.example.rewards.model.Project;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class ProjectRouter {

        @Bean
        @RouterOperations({
                        @RouterOperation(path = "/api/projects", method = RequestMethod.GET, beanClass = ProjectHandler.class, beanMethod = "getAllProjects", operation = @Operation(operationId = "getAllProjects", summary = "Get all projects", description = "Retrieve a list of all projects", tags = {
                                        "Projects" }, responses = {
                                                        @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Project.class)))
                                        })),
                        @RouterOperation(path = "/api/projects/{id}", method = RequestMethod.GET, beanClass = ProjectHandler.class, beanMethod = "getById", operation = @Operation(operationId = "getProjectById", summary = "Find project by ID", description = "Returns a single project", tags = {
                                        "Projects" }, parameters = {
                                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID of project to return", schema = @Schema(type = "string"))
                                        }, responses = {
                                                        @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Project.class))),
                                                        @ApiResponse(responseCode = "404", description = "Project not found")
                                        })),
                        @RouterOperation(path = "/api/projects", method = RequestMethod.POST, beanClass = ProjectHandler.class, beanMethod = "create", operation = @Operation(operationId = "createProject", summary = "Create a new project", description = "Add a new project to the system", tags = {
                                        "Projects" }, requestBody = @RequestBody(required = true, description = "Project object that needs to be added", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Project.class))), responses = {
                                                        @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Project.class)))
                                        })),
                        @RouterOperation(path = "/api/projects/{id}", method = RequestMethod.PUT, beanClass = ProjectHandler.class, beanMethod = "update", operation = @Operation(operationId = "updateProject", summary = "Update an existing project", description = "Update a project by ID", tags = {
                                        "Projects" }, parameters = {
                                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID of project to update", schema = @Schema(type = "string"))
                                        }, requestBody = @RequestBody(required = true, description = "Updated project object", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Project.class))), responses = {
                                                        @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Project.class))),
                                                        @ApiResponse(responseCode = "404", description = "Project not found")
                                        })),
                        @RouterOperation(path = "/api/projects/{id}", method = RequestMethod.DELETE, beanClass = ProjectHandler.class, beanMethod = "delete", operation = @Operation(operationId = "deleteProject", summary = "Delete a project", description = "Delete a project by ID", tags = {
                                        "Projects" }, parameters = {
                                                        @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID of project to delete", schema = @Schema(type = "string"))
                                        }, responses = {
                                                        @ApiResponse(responseCode = "204", description = "Successful operation"),
                                                        @ApiResponse(responseCode = "404", description = "Project not found")
                                        }))
        })
        public RouterFunction<ServerResponse> projectRoutes(ProjectHandler handler) {
                return RouterFunctions
                                .route(GET("/api/projects").and(accept(MediaType.APPLICATION_JSON)),
                                                handler::getAllProjects)
                                .andRoute(GET("/api/projects/{id}").and(accept(MediaType.APPLICATION_JSON)),
                                                handler::getById)
                                .andRoute(org.springframework.web.reactive.function.server.RequestPredicates
                                                .POST("/api/projects")
                                                .and(accept(MediaType.APPLICATION_JSON)), handler::create)
                                .andRoute(org.springframework.web.reactive.function.server.RequestPredicates
                                                .PUT("/api/projects/{id}")
                                                .and(accept(MediaType.APPLICATION_JSON)), handler::update)
                                .andRoute(org.springframework.web.reactive.function.server.RequestPredicates
                                                .DELETE("/api/projects/{id}").and(accept(MediaType.APPLICATION_JSON)),
                                                handler::delete);
        }
}
