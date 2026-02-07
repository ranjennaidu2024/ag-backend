package com.example.rewards.api;

import com.example.rewards.model.Reward;
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

@Configuration
public class RewardRouter {

  @Bean
  @RouterOperations({
    @RouterOperation(
      path = "/api/rewards",
      method = RequestMethod.GET,
      beanClass = RewardHandler.class,
      beanMethod = "getAll",
      operation = @Operation(
        operationId = "getAllRewards",
        summary = "Get all rewards",
        description = "Retrieve a list of all rewards",
        tags = {"Rewards"},
        responses = {
          @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Reward.class)
            )
          )
        }
      )
    ),
    @RouterOperation(
      path = "/api/rewards/{id}",
      method = RequestMethod.GET,
      beanClass = RewardHandler.class,
      beanMethod = "getById",
      operation = @Operation(
        operationId = "getRewardById",
        summary = "Find reward by ID",
        description = "Returns a single reward",
        tags = {"Rewards"},
        parameters = {
          @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true,
            description = "ID of reward to return",
            schema = @Schema(type = "string")
          )
        },
        responses = {
          @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Reward.class)
            )
          ),
          @ApiResponse(
            responseCode = "404",
            description = "Reward not found"
          )
        }
      )
    ),
    @RouterOperation(
      path = "/api/rewards/user/{userId}",
      method = RequestMethod.GET,
      beanClass = RewardHandler.class,
      beanMethod = "getByUser",
      operation = @Operation(
        operationId = "getRewardsByUser",
        summary = "Find rewards by user ID",
        description = "Returns all rewards for a specific user",
        tags = {"Rewards"},
        parameters = {
          @Parameter(
            name = "userId",
            in = ParameterIn.PATH,
            required = true,
            description = "User ID to find rewards for",
            schema = @Schema(type = "string")
          )
        },
        responses = {
          @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Reward.class)
            )
          )
        }
      )
    ),
    @RouterOperation(
      path = "/api/rewards",
      method = RequestMethod.POST,
      beanClass = RewardHandler.class,
      beanMethod = "create",
      operation = @Operation(
        operationId = "createReward",
        summary = "Create a new reward",
        description = "Add a new reward to the system",
        tags = {"Rewards"},
        requestBody = @RequestBody(
          required = true,
          description = "Reward object that needs to be added",
          content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = Reward.class)
          )
        ),
        responses = {
          @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Reward.class)
            )
          ),
          @ApiResponse(
            responseCode = "400",
            description = "Invalid input"
          )
        }
      )
    ),
    @RouterOperation(
      path = "/api/rewards/{id}",
      method = RequestMethod.PUT,
      beanClass = RewardHandler.class,
      beanMethod = "update",
      operation = @Operation(
        operationId = "updateReward",
        summary = "Update an existing reward",
        description = "Update a reward by ID",
        tags = {"Rewards"},
        parameters = {
          @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true,
            description = "ID of reward to update",
            schema = @Schema(type = "string")
          )
        },
        requestBody = @RequestBody(
          required = true,
          description = "Updated reward object",
          content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = Reward.class)
          )
        ),
        responses = {
          @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = Reward.class)
            )
          ),
          @ApiResponse(
            responseCode = "404",
            description = "Reward not found"
          ),
          @ApiResponse(
            responseCode = "400",
            description = "Invalid input"
          )
        }
      )
    ),
    @RouterOperation(
      path = "/api/rewards/{id}",
      method = RequestMethod.DELETE,
      beanClass = RewardHandler.class,
      beanMethod = "delete",
      operation = @Operation(
        operationId = "deleteReward",
        summary = "Delete a reward",
        description = "Delete a reward by ID",
        tags = {"Rewards"},
        parameters = {
          @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true,
            description = "ID of reward to delete",
            schema = @Schema(type = "string")
          )
        },
        responses = {
          @ApiResponse(
            responseCode = "204",
            description = "Successful operation"
          ),
          @ApiResponse(
            responseCode = "404",
            description = "Reward not found"
          )
        }
      )
    )
  })
  public RouterFunction<ServerResponse> routes(RewardHandler handler) {
    return RouterFunctions.route()
      .GET("/api/rewards", handler::getAll)
      .GET("/api/rewards/{id}", handler::getById)
      .GET("/api/rewards/user/{userId}", handler::getByUser)
      .POST("/api/rewards", handler::create)
      .PUT("/api/rewards/{id}", handler::update)
      .DELETE("/api/rewards/{id}", handler::delete)
      .build();
  }
}

