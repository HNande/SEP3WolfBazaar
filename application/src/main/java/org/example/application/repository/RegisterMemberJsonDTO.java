package org.example.application.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterMemberJsonDTO(
        @NotBlank(message = "First name is required")
        @Size(max = MemberConstraints.NAME_MAX,
                message = "First name must be at most {max} characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = MemberConstraints.NAME_MAX,
                message = "Last name must be at most {max} characters")
        String lastName,

        @NotBlank(message = "Username is required")
        @Size(max = MemberConstraints.USERNAME_MAX,
                message = "Username must be at most {max} characters")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
    @Override
    public String toString() {
        return "RegisterMemberJsonDTO[firstName=" + firstName + ", lastName=" + lastName
                + ", username=" + username + ", password=***]";
    }
}
