package com.clinicare.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.clinicare.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ← ADD THIS
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.managedClinic WHERE u.email = :email")
    Optional<User> findByEmailWithClinic(@Param("email") String email);
}