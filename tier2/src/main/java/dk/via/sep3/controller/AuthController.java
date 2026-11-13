package dk.via.sep3.controller;

import dk.via.sep3.exception.ApiExceptionHandler.ErrorResponse;
import dk.via.sep3.repository.LoginMemberJsonDTO;
import dk.via.sep3.repository.LoginResponseDTO;
import dk.via.sep3.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    // Log a member in. 200 if the credentials are correct, 401 if not
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginMemberJsonDTO form) {
        if (!memberService.login(form)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Wrong user name or password"));
        }

        return ResponseEntity.ok(new LoginResponseDTO(form.username().trim()));
    }
}
