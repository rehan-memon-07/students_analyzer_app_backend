package com.ai.career.backend.controller;

import com.ai.career.backend.dto.SessionInitRequest;
import com.ai.career.backend.model.User;
import com.ai.career.backend.repository.UserRepository;
import com.ai.career.backend.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/session")
public class SessionController {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    public SessionController(UserRepository userRepository,
                             SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    // Intialize session for user 

    @PostMapping("/init")
    public String initSession(@RequestBody SessionInitRequest request) {

        User user = userRepository
                .findByExternalUserId(request.getExternalUserId())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .externalUserId(request.getExternalUserId())
                                .email(request.getEmail())
                                .createdAt(Instant.now())
                                .build()
                ));

        return sessionService.createSession(user.getId());
    }
}
