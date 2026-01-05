package com.ninjaone.dundie_awards.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;

/** 
 * Record representing organization information 
 * 
 * id - the unique identifier of the organization. It defaults to 0 if null and should be generated when saved
 * name - the name of the organization 
 */
@Builder(toBuilder = true)
public record OrganizationInfo(
    @NotNull Long id,
    @NotBlank @NonNull String name) {

    public OrganizationInfo {
        if (id == null) {
            id = 0L;
        }
    }
}