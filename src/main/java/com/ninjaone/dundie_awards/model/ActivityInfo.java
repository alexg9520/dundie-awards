package com.ninjaone.dundie_awards.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.NonNull;

/** Record representing activity information */
@Builder(toBuilder = true)
public record ActivityInfo(
    Long id,
    @NonNull LocalDateTime occuredAt, 
    @NonNull String event) {
}
