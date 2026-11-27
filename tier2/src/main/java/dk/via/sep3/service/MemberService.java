package dk.via.sep3.service;

import dk.via.sep3.dto.LoginMemberJsonDTO;
import dk.via.sep3.dto.MemberConstraints;
import dk.via.sep3.dto.MemberResponseDTO;
import dk.via.sep3.dto.RegisterMemberJsonDTO;
import dk.via.sep3.client.MemberGrpcClient;
import dk.via.sep3.generated.AddMemberRequest;
import dk.via.sep3.generated.LoginRequest;
import dk.via.sep3.generated.Member;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberGrpcClient memberClient;

    public MemberService(MemberGrpcClient memberClient) {
        this.memberClient = memberClient;
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
                .setMemberEmail(dto.email().trim())
                .setPassword(dto.password())
                .build();

        return memberClient.addMember(request)
                .map(response -> toDto(response.getMember(), request.getFirstName(), request.getLastName()));
    }

    public Optional<MemberResponseDTO> login(LoginMemberJsonDTO dto) {
        LoginRequest request = LoginRequest.newBuilder()
                .setMemberName(dto.username().trim())
                .setPassword(dto.password())
                .build();

        return memberClient.login(request)
                .map(response -> toDto(response.getMember(), "", ""));
    }

    private static MemberResponseDTO toDto(Member member, String firstName, String lastName) {
        return new MemberResponseDTO(
                member.getId(),
                member.getMemberName(),
                member.getMemberEmail(),
                firstName,
                lastName);
    }
}