package com.jobpulse.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password; // BCrypt encoded

    private String email;
    @Column(name = "full_name")
    private String fullName;
    @Column(name = "role")
    private String role; // USER, ADMIN, WORKER

    @Column(name = "is_active")
    private boolean enabled = true;
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name="target_roles") private String targetRoles;
    @Column(name="target_locations") private String targetLocations;
    public String getTargetRoles() { return targetRoles; }
    public void setTargetRoles(String value) { targetRoles=value; }
    public String getTargetLocations() { return targetLocations; }
    public void setTargetLocations(String value) { targetLocations=value; }

    @Column(name="token_version", nullable=false) private long tokenVersion;
    public long getTokenVersion() {return tokenVersion;}
    public void setTokenVersion(long value) {tokenVersion=value;}

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

