package com.ninjaone.dundie_awards.model;

import lombok.Builder;
import lombok.NonNull;

/** Record representing organization information */
@Builder
public record OrganizationInfo(
    Long id,
    @NonNull String name) {
}