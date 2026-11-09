package org.example.application.controller;

import jakarta.validation.Valid;
import org.example.application.exception.ApiExceptionHandler.ErrorResponse;
import org.example.application.repository.MemberResponseDTO;
import org.example.application.repository.RegisterMemberJsonDTO;
import org.example.application.service.MemberService;
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

    //Register a guest as a new member. 201 if created, 409 if the username is taken.
    @PostMapping("")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterMemberJsonDTO form) {
        Optional<MemberResponseDTO> member = memberService.register(form);

        if (member.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Username is already taken"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(member.get());
    }
}