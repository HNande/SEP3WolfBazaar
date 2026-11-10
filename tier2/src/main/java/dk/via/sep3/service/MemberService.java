package dk.via.sep3.service;

import dk.via.sep3.generated.*;
import dk.via.sep3.client.MemberGrpcClient;
import dk.via.sep3.repository.MemberConstraints;
import dk.via.sep3.repository.MemberResponseDTO;
import dk.via.sep3.repository.RegisterMemberJsonDTO;
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

    public Optional<String> validatePassword(String password) {
        if (password.length() < MemberConstraints.PASSWORD_MIN) {
            return Optional.of(
                    "Password must be at least " + MemberConstraints.PASSWORD_MIN + " characters long");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MemberConstraints.PASSWORD_MAX_BYTES) {
            return Optional.of(
                    "Password must be at most " + MemberConstraints.PASSWORD_MAX_BYTES + " bytes long");
        }
        return Optional.empty();
    }

    public Optional<MemberResponseDTO> register(RegisterMemberJsonDTO dto) {
        AddMemberRequest request = AddMemberRequest.newBuilder()
                .setFirstName(dto.firstName().trim())
                .setLastName(dto.lastName().trim())
                .setMemberName(dto.username().trim())
                .setPassword(passwordEncoder.encode(dto.password()))
                .build();

        if (!memberClient.addMember(request)) {
            return Optional.empty();
        }

        return Optional.of(new MemberResponseDTO(
                request.getFirstName(), request.getLastName(), request.getMemberName()));
    }
}