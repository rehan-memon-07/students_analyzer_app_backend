package com.ai.career.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service 
public class SessionService {
    private final Map<String , UUID>  session = new ConcurrentHashMap<>();

    public String createSession(UUID userId) {
        String token = UUID.randomUUID().toString();
        session.put(token , userId);
        return token;
    }

    public UUID getUserFromSession(String token){
        return session.get(token);
    }
}