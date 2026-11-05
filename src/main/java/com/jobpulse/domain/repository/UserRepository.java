package com.jobpulse.domain.repository;

import com.jobpulse.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.username = :username")
    Optional<User> lockByUsername(@org.springframework.data.repository.query.Param("username") String username);
    Optional<User> findByUsername(String username);
}

