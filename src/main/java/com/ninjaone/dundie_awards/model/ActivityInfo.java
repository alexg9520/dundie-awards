package com.ninjaone.dundie_awards.model;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;

/** 
 * Record representing activity information 
 * 
 * id - the unique identifier of the activity. It defaults to 0 if null and should be generated when saved
 * occuredAt - the date and time when the activity occurred
 * event - a description of the activity event
 */
@Builder(toBuilder = true)
public record ActivityInfo(
    @NotNull Long id,
    @NotNull @NonNull LocalDateTime occuredAt, 
    @NotBlank @NonNull String event) {

    public ActivityInfo {
        if (id == null) {
            id = 0L;
        }
    }
}