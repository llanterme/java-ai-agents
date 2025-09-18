package za.co.digitalcowboy.agents.domain.social.dto;

import jakarta.validation.constraints.NotNull;

public class SocialPostRequest {
    @NotNull(message = "Content ID is required")
    private Long id;

    public SocialPostRequest() {}

    public SocialPostRequest(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}