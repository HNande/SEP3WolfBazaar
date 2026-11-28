package dk.via.sep3.mapper;

import dk.via.sep3.dto.LoginMemberJsonDTO;
import dk.via.sep3.dto.MemberResponseDTO;
import dk.via.sep3.dto.RegisterMemberJsonDTO;
import dk.via.sep3.generated.Member;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberMapperTest {

    @Test
    void toMemberResponseMapsEveryField() {
        Member member = Member.newBuilder()
                .setId(3)
                .setMemberName("ada")
                .setMemberEmail("ada@example.com")
                .setFirstName("Ada")
                .setLastName("Lovelace")
                .build();

        assertEquals(new MemberResponseDTO(3, "ada", "ada@example.com", "Ada", "Lovelace"),
                MemberMapper.toMemberResponseDTO(member));
    }

    @Test
    void addMemberRequestTrimsIdentityFieldsAndPreservesPassword() {
        RegisterMemberJsonDTO dto = new RegisterMemberJsonDTO(
                " Ada ", " Lovelace ", " ada ", " ada@example.com ", "secret123");

        var request = MemberMapper.toAddMemberRequest(dto);

        assertEquals("Ada", request.getFirstName());
        assertEquals("Lovelace", request.getLastName());
        assertEquals("ada", request.getMemberName());
        assertEquals("ada@example.com", request.getMemberEmail());
        assertEquals("secret123", request.getPassword());
    }

    @Test
    void loginRequestTrimsUsernameAndPreservesPassword() {
        var request = MemberMapper.toLoginRequest(new LoginMemberJsonDTO(" ada ", "secret123"));

        assertEquals("ada", request.getMemberName());
        assertEquals("secret123", request.getPassword());
    }
}