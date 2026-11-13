package dk.via.sep3.controller;

import dk.via.sep3.exception.ApiExceptionHandler;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.repository.MemberResponseDTO;
import dk.via.sep3.repository.RegisterMemberJsonDTO;
import dk.via.sep3.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest {

    private MemberService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(MemberService.class);
        mvc = MockMvcBuilders.standaloneSetup(new MemberController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static String body(String first, String last, String user, String pass) {
        return """
            {"firstName":%s,"lastName":%s,"username":%s,"password":%s}
            """.formatted(q(first), q(last), q(user), q(pass));
    }

    private static String q(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @Nested
    @DisplayName("POST /api/member")
    class RegisterTests {

        @Test
        @DisplayName("returns 201 with the member and no password")
        void one_validRequest_returns201() throws Exception {
            when(service.validatePassword(anyString())).thenReturn(Optional.empty());
            when(service.register(any(RegisterMemberJsonDTO.class)))
                    .thenReturn(Optional.of(new MemberResponseDTO("Fenrir", "Wolf", "fenrir99")));

            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", "Wolf", "fenrir99", "password123")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstName").value("Fenrir"))
                    .andExpect(jsonPath("$.lastName").value("Wolf"))
                    .andExpect(jsonPath("$.username").value("fenrir99"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("returns 400 when first name is blank")
        void zero_blankFirstName_returns400() throws Exception {
            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("", "Wolf", "fenrir99", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("First name is required")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 400 when last name is missing")
        void zero_missingLastName_returns400() throws Exception {
            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", null, "fenrir99", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Last name is required")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void zero_blankUsername_returns400() throws Exception {
            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", "Wolf", "  ", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Username is required")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 400 when password is missing")
        void zero_missingPassword_returns400() throws Exception {
            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", "Wolf", "fenrir99", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Password is required")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 400 with the message when the password is invalid, without registering")
        void boundary_invalidPassword_returns400() throws Exception {
            when(service.validatePassword("short12"))
                    .thenReturn(Optional.of("Password must be at least 8 characters long"));

            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", "Wolf", "fenrir99", "short12")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long"));

            verify(service, never()).register(any());
        }

        @Test
        @DisplayName("returns 409 when the username is already taken")
        void boundary_usernameTaken_returns409() throws Exception {
            when(service.validatePassword(anyString())).thenReturn(Optional.empty());
            when(service.register(any(RegisterMemberJsonDTO.class))).thenReturn(Optional.empty());

            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", "Wolf", "fenrir99", "password123")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Username is already taken"));
        }

        @Test
        @DisplayName("returns 400 for malformed JSON")
        void exceptional_malformedJson_returns400() throws Exception {
            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"firstName\": "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 502 when tier 3 fails")
        void exceptional_upstreamFails_returns502() throws Exception {
            when(service.validatePassword(anyString())).thenReturn(Optional.empty());
            when(service.register(any(RegisterMemberJsonDTO.class)))
                    .thenThrow(new UpstreamException("Could not add member: UNAVAILABLE", null));

            mvc.perform(post("/api/member").contentType(MediaType.APPLICATION_JSON)
                            .content(body("Fenrir", "Wolf", "fenrir99", "password123")))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502));
        }
    }
}