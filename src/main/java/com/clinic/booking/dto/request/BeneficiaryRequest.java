package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryRequest {

    @NotBlank(message = "name is required")
    private String name;

    /** Optional, e.g. "Self", "Mother". */
    private String relation;
}