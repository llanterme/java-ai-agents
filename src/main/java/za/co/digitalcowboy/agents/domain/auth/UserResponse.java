package za.co.digitalcowboy.agents.domain.auth;

import za.co.digitalcowboy.agents.domain.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    Long id,
    String email,
    String name,
    String surname,
    List<String> roles,
    Boolean active,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getSurname(),
            List.of("USER"),
            user.getActive(),
            user.getCreatedAt()
        );
    }
}