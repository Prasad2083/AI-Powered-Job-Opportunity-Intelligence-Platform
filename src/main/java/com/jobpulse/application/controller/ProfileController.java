package com.jobpulse.application.controller;

import com.jobpulse.domain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserRepository users;
    public ProfileController(UserRepository users) {this.users=users;}
    public record Preferences(@Size(max=255) String fullName,@Size(max=1000) String targetRoles,@Size(max=255) String targetLocations) {}
    @GetMapping public Map<String,Object> get(Authentication auth) {
        var u=users.findByUsername(auth.getName()).orElseThrow();
        return Map.of("username",u.getUsername(),"email",u.getEmail(),"fullName",u.getFullName()==null?"":u.getFullName(),
            "targetRoles",u.getTargetRoles()==null?"Senior Quality Engineer,Lead Quality Engineer,SDET,Test Automation Engineer,Backend Java Developer,AI Quality Engineer":u.getTargetRoles(),
            "targetLocations",u.getTargetLocations()==null?"Toronto,Greater Toronto Area,Remote Canada,Hybrid":u.getTargetLocations());
    }
    @PutMapping public Map<String,Object> save(@Valid @RequestBody Preferences input,Authentication auth) {
        var u=users.findByUsername(auth.getName()).orElseThrow();
        u.setFullName(input.fullName());u.setTargetRoles(input.targetRoles());u.setTargetLocations(input.targetLocations());users.save(u);return get(auth);
    }
}
