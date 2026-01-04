package com.ninjaone.dundie_awards.model;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;

/** Record representing activity information */
@Builder(toBuilder = true)
public record ActivityInfo(
    Long id,
    @NotNull @NonNull LocalDateTime occuredAt, 
    @NotBlank @NonNull String event) {
}
