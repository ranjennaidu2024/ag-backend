package com.example.rewards.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("rewards")
@Schema(description = "Reward entity representing user rewards")
public class Reward {
  @Id
  @Schema(description = "Unique identifier of the reward", example = "507f1f77bcf86cd799439011", accessMode = Schema.AccessMode.READ_ONLY)
  private String id;

  @NotBlank
  @Schema(description = "User ID associated with the reward", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
  private String userId;

  @Min(0)
  @Schema(description = "Number of reward points", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
  private int points;

  @Schema(description = "Description of the reward", example = "Completed a challenge")
  private String description;

  public Reward() {
  }

  public Reward(String id, String userId, int points, String description) {
    this.id = id;
    this.userId = userId;
    this.points = points;
    this.description = description;
  }

  public Reward(String userId, int points, String description) {
    this(null, userId, points, description);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

