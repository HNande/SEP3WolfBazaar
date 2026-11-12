package dk.via.sep3.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginMemberJsonDTO(
        @NotBlank(message = "Username is required")
        @Size(max = MemberConstraints.USERNAME_MAX,
                message = "Username must be at most {max} characters")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
    @Override
    public String toString() {
        return "LoginMemberJsonDTO[username=" + username + ", password=***]";
    }
}
