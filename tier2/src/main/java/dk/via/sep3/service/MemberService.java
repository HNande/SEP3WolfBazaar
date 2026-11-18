package dk.via.sep3.service;

import dk.via.sep3.generated.*;
import dk.via.sep3.client.MemberGrpcClient;
import dk.via.sep3.dto.LoginMemberJsonDTO;
import dk.via.sep3.dto.MemberConstraints;
import dk.via.sep3.dto.MemberResponseDTO;
import dk.via.sep3.dto.RegisterMemberJsonDTO;
import dk.via.sep3.mapper.MemberMapper;
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
        AddMemberRequest request = MemberMapper.toAddMemberRequest(dto);
        dk.via.sep3.generated.Member member = memberClient.addMember(request).get().getMember();
        return Optional.of(MemberMapper.toMemberResponseDTO(member));
    }

    public Optional<MemberResponseDTO> login(LoginMemberJsonDTO dto) {
        LoginRequest request = MemberMapper.toLoginRequest(dto);
        Optional<MemberResponse> response = memberClient.login(request);
        if (response.isEmpty()) {
            return Optional.empty();
        }
        dk.via.sep3.generated.Member member = response.get().getMember();
        return Optional.of(MemberMapper.toMemberResponseDTO(member));
    }
}