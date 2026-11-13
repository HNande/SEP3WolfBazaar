package dk.via.sep3.controller;

import dk.via.sep3.exception.ApiExceptionHandler;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.repository.LoginMemberJsonDTO;
import dk.via.sep3.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MemberService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(MemberService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static String body(String user, String pass) {
        return """
            {"username":%s,"password":%s}
            """.formatted(q(user), q(pass));
    }

    private static String q(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("returns 200 with the trimmed username as name")
        void one_validCredentials_returns200() throws Exception {
            when(service.login(any(LoginMemberJsonDTO.class))).thenReturn(true);

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body(" fenrir99 ", "password123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("fenrir99"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("returns 401 when the credentials are wrong")
        void boundary_wrongCredentials_returns401() throws Exception {
            when(service.login(any(LoginMemberJsonDTO.class))).thenReturn(false);

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body("fenrir99", "wrongpassword")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Wrong user name or password"));
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void zero_blankUsername_returns400() throws Exception {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body("", "password123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Username is required")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 400 when password is missing")
        void zero_missingPassword_returns400() throws Exception {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body("fenrir99", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Password is required")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 400 for the old field name 'name' instead of 'username'")
        void exceptional_wrongFieldName_returns400() throws Exception {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"fenrir99\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("returns 502 when tier 3 fails")
        void exceptional_upstreamFails_returns502() throws Exception {
            when(service.login(any(LoginMemberJsonDTO.class)))
                    .thenThrow(new UpstreamException("Could not login: UNAVAILABLE", null));

            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body("fenrir99", "password123")))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502));
        }
    }
}