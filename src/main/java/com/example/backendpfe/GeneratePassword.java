package com.example.backendpfe;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder(12).encode("admin123456"));
    }
}