package dk.via.sep3.controller;

import jakarta.validation.Valid;
import dk.via.sep3.exception.ApiExceptionHandler.ErrorResponse;
import dk.via.sep3.repository.MemberResponseDTO;
import dk.via.sep3.repository.RegisterMemberJsonDTO;
import dk.via.sep3.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // Register a guest as a new member. 201 if created, 400 if invalid, 409 if the username is taken
    @PostMapping("")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterMemberJsonDTO form) {
        Optional<String> passwordError = memberService.validatePassword(form.password());
        if (passwordError.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), passwordError.get()));
        }

        Optional<MemberResponseDTO> member = memberService.register(form);
        if (member.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Username is already taken"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(member.get());
    }
}