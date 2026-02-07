package com.example.rewards.model;

// import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "projects")
// @Schema(description = "Project details")
public class Project {

    @Id
    // @Schema(description = "Unique identifier of the project", example =
    // "60d5ec9af682fbd39a1b8b9d")
    private String id;

    // @Schema(description = "Name of the project", example = "Project Alpha")
    private String name;

    // @Schema(description = "Current status of the project", example = "Running",
    // allowableValues = { "Running", "Ended",
    // "Pending" })
    private String status; // e.g., "Ended", "Running", "Pending"

    // @Schema(description = "Type of the project", example = "Web App")
    private String type; // e.g., "Web App", "Mobile App"

    // @Schema(description = "Progress percentage", example = "75", minimum = "0",
    // maximum = "100")
    private int progress; // 0-100

    public Project() {
    }

    public Project(String name, String status, String type, int progress) {
        this.name = name;
        this.status = status;
        this.type = type;
        this.progress = progress;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
