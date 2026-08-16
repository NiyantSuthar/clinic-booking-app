package com.clinic.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * passwordHash is new this session - nullable. Null means "hasn't
 * completed registration yet" (brand-new number, or a legacy account
 * from before this session existed) - AuthService.login() treats a null
 * hash as the signal to start the OTP-registration flow rather than
 * expecting a password. Never store the raw password, only the BCrypt hash.
 */
@Entity
@Table(name = "account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column
    private String name;

    @Column
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "expo_push_token")
    private String expoPushToken;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}