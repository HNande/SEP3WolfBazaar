package dk.via.sep3.service;

import dk.via.sep3.client.MemberGrpcClient;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.generated.AddMemberRequest;
import dk.via.sep3.generated.LoginRequest;
import dk.via.sep3.repository.LoginMemberJsonDTO;
import dk.via.sep3.repository.MemberResponseDTO;
import dk.via.sep3.repository.RegisterMemberJsonDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemberServiceTest {

    private MemberGrpcClient client;
    private PasswordEncoder encoder;
    private MemberService service;

    @BeforeEach
    void setUp() {
        client = mock(MemberGrpcClient.class);
        encoder = new BCryptPasswordEncoder();
        service = new MemberService(client, encoder);
    }

    @Nested
    @DisplayName("validatePassword")
    class ValidatePasswordTests {

        @Test
        @DisplayName("rejects an empty password")
        void zero_emptyPassword_returnsError() {
            assertTrue(service.validatePassword("").isPresent());
        }

        @Test
        @DisplayName("rejects a password with 7 characters")
        void one_sevenCharacters_returnsError() {
            Optional<String> result = service.validatePassword("1234567");

            assertTrue(result.isPresent());
            assertEquals("Password must be at least 8 characters long", result.get());
        }

        @Test
        @DisplayName("accepts a password with exactly 8 characters")
        void boundary_eightCharacters_isValid() {
            assertTrue(service.validatePassword("12345678").isEmpty());
        }

        @Test
        @DisplayName("accepts a password with exactly 72 bytes")
        void boundary_72Bytes_isValid() {
            assertTrue(service.validatePassword("a".repeat(72)).isEmpty());
        }

        @Test
        @DisplayName("rejects a password with 73 bytes")
        void boundary_73Bytes_returnsError() {
            Optional<String> result = service.validatePassword("a".repeat(73));

            assertTrue(result.isPresent());
            assertEquals("Password must be at most 72 bytes long", result.get());
        }

        @Test
        @DisplayName("counts bytes, not characters, for multi-byte characters")
        void boundary_multiByteCharactersOver72Bytes_returnsError() {
            assertTrue(service.validatePassword("\u00e5".repeat(37)).isPresent());
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        private RegisterMemberJsonDTO dto;

        @BeforeEach
        void setUp() {
            dto = new RegisterMemberJsonDTO(" Fenrir ", " Wolf ", " fenrir99 ", "password123");
        }

        @Test
        @DisplayName("returns the new member with trimmed fields")
        void one_validDto_returnsMemberWithTrimmedFields() {
            when(client.addMember(any(AddMemberRequest.class))).thenReturn(true);

            Optional<MemberResponseDTO> result = service.register(dto);

            assertTrue(result.isPresent());
            assertEquals(new MemberResponseDTO("Fenrir", "Wolf", "fenrir99"), result.get());
        }

        @Test
        @DisplayName("sends trimmed fields and a hashed password to tier 3")
        void one_validDto_sendsTrimmedFieldsAndHashedPassword() {
            when(client.addMember(any(AddMemberRequest.class))).thenReturn(true);

            service.register(dto);

            ArgumentCaptor<AddMemberRequest> captor = ArgumentCaptor.forClass(AddMemberRequest.class);
            verify(client).addMember(captor.capture());
            AddMemberRequest sent = captor.getValue();

            assertEquals("Fenrir", sent.getFirstName());
            assertEquals("Wolf", sent.getLastName());
            assertEquals("fenrir99", sent.getMemberName());
            assertNotEquals("password123", sent.getPassword());
            assertTrue(encoder.matches("password123", sent.getPassword()));
        }

        @Test
        @DisplayName("returns empty when the username is already taken")
        void boundary_usernameTaken_returnsEmpty() {
            when(client.addMember(any(AddMemberRequest.class))).thenReturn(false);

            assertTrue(service.register(dto).isEmpty());
        }

        @Test
        @DisplayName("propagates UpstreamException from the client")
        void exceptional_clientThrows_propagates() {
            when(client.addMember(any(AddMemberRequest.class)))
                    .thenThrow(new UpstreamException("Could not add member: UNAVAILABLE", null));

            assertThrows(UpstreamException.class, () -> service.register(dto));
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        private LoginMemberJsonDTO dto;

        @BeforeEach
        void setUp() {
            dto = new LoginMemberJsonDTO(" fenrir99 ", "password123");
        }

        @Test
        @DisplayName("returns true and sends the trimmed username with the plain password")
        void one_validCredentials_returnsTrue() {
            when(client.login(any(LoginRequest.class))).thenReturn(true);

            boolean result = service.login(dto);

            assertTrue(result);
            ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
            verify(client).login(captor.capture());
            assertEquals("fenrir99", captor.getValue().getMemberName());
            assertEquals("password123", captor.getValue().getPassword());
        }

        @Test
        @DisplayName("returns false when the credentials are wrong")
        void boundary_wrongCredentials_returnsFalse() {
            when(client.login(any(LoginRequest.class))).thenReturn(false);

            assertFalse(service.login(dto));
        }

        @Test
        @DisplayName("propagates UpstreamException from the client")
        void exceptional_clientThrows_propagates() {
            when(client.login(any(LoginRequest.class)))
                    .thenThrow(new UpstreamException("Could not login: UNAVAILABLE", null));

            assertThrows(UpstreamException.class, () -> service.login(dto));
        }
    }
}