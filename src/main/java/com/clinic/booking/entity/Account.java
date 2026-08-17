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
 * email replaced with village this session (client change #1). Nullable
 * at the DB level deliberately - required-ness is enforced at the DTO/
 * validation layer instead (see UpdateAccountRequest), not a DB NOT NULL
 * constraint, since adding a hard NOT NULL to an existing populated
 * table can fail on deploy if any current row has no value yet.
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
    private String village;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "expo_push_token")
    private String expoPushToken;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}