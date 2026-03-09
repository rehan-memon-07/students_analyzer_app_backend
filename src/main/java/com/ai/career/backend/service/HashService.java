package com.ai.career.backend.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;

@Service
public class HashService {

    public String generateHash(String input) {
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating SHA256 hash", e);
        }
    }
}
