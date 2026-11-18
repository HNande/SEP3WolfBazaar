package dk.via.sep3.mapper;

import dk.via.sep3.generated.*;
import dk.via.sep3.dto.LoginMemberJsonDTO;
import dk.via.sep3.dto.MemberResponseDTO;
import dk.via.sep3.dto.RegisterMemberJsonDTO;

public final class MemberMapper {

    private MemberMapper() {
    }

    public static MemberResponseDTO toMemberResponseDTO(Member member) {
        return new MemberResponseDTO(
                member.getId(),
                member.getMemberName(),
                member.getMemberEmail(),
                member.getFirstName(),
                member.getLastName());
    }

    public static AddMemberRequest toAddMemberRequest(RegisterMemberJsonDTO dto) {
        return AddMemberRequest.newBuilder()
                .setFirstName(dto.firstName().trim())
                .setLastName(dto.lastName().trim())
                .setMemberName(dto.username().trim())
                .setMemberEmail(dto.email().trim())
                .setPassword(dto.password())
                .build();
    }

    public static LoginRequest toLoginRequest(LoginMemberJsonDTO dto) {
        return LoginRequest.newBuilder()
                .setMemberName(dto.username().trim())
                .setPassword(dto.password())
                .build();
    }
}
