package org.example.application.service;

import generated.AddMemberRequest;
import org.example.application.exception.InvalidRequestException;
import org.example.application.client.MemberGrpcClient;
import org.example.application.repository.MemberConstraints;
import org.example.application.repository.MemberResponseDTO;
import org.example.application.repository.RegisterMemberJsonDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberGrpcClient memberClient;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberGrpcClient memberClient, PasswordEncoder passwordEncoder) {
        this.memberClient = memberClient;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<MemberResponseDTO> register(RegisterMemberJsonDTO dto) {
        String firstName = dto.firstName().trim();
        String lastName = dto.lastName().trim();
        String username = dto.username().trim();
        String password = dto.password();

        validatePassword(password);

        AddMemberRequest request = AddMemberRequest.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setMemberName(username)
                .setPassword(passwordEncoder.encode(password))
                .build();

        if (!memberClient.addMember(request)) {
            return Optional.empty();
        }

        return Optional.of(new MemberResponseDTO(firstName, lastName, username));
    }

    private static void validatePassword(String password) {
        if (password.length() < MemberConstraints.PASSWORD_MIN) {
            throw new InvalidRequestException(
                    "Password must be at least " + MemberConstraints.PASSWORD_MIN + " characters long");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MemberConstraints.PASSWORD_MAX_BYTES) {
            throw new InvalidRequestException(
                    "Password must be at most " + MemberConstraints.PASSWORD_MAX_BYTES + " bytes long");
        }
    }
}