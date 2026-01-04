package com.ninjaone.dundie_awards.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.NonNull;

/** Record representing organization information */
@Builder
public record OrganizationInfo(
    Long id,
    @NotBlank @NonNull String name) {
}