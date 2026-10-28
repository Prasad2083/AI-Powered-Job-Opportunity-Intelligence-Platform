package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.User;
import com.jobpulse.domain.repository.UserRepository;
import com.jobpulse.common.util.JwtTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.Map;
import java.util.Optional;

/**
 * Phase 5: Authentication Controller - Register, Login, JWT tokens
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setFullName(req.getFullName());
        user.setRole("USER");
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "username", saved.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        if (!user.isEnabled() || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtTokenUtil.generateToken(user.getUsername(), user.getRole(), user.getTokenVersion());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> logout(org.springframework.security.core.Authentication auth) {
        var user=userRepository.lockByUsername(auth.getName()).orElseThrow();
        user.setTokenVersion(user.getTokenVersion()+1);userRepository.save(user);
        return ResponseEntity.ok(Map.of("message","All sessions revoked"));
    }

    public static class RegisterRequest {
        @NotBlank @jakarta.validation.constraints.Size(max=100) public String username;
        @NotBlank @jakarta.validation.constraints.Size(min=12,max=72) public String password;
        @Email @NotBlank @jakarta.validation.constraints.Size(max=255) public String email;
        @jakarta.validation.constraints.Size(max=255) public String fullName;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    public static class LoginRequest {
        @NotBlank @jakarta.validation.constraints.Size(max=100) public String username;
        @NotBlank @jakarta.validation.constraints.Size(max=72) public String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}

