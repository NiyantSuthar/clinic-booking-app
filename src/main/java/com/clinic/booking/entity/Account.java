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
 * expoPushToken is new this session - nullable, since it's only populated
 * once the app successfully registers for push notifications (which
 * itself depends on the user granting permission, and currently only
 * fully resolves on a real device build, not Expo Go - see NotificationService).
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

    @Column(name = "expo_push_token")
    private String expoPushToken;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}