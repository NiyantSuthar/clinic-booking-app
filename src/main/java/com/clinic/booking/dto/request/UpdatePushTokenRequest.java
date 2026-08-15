package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePushTokenRequest {

    @NotBlank(message = "expoPushToken is required")
    private String expoPushToken;
}