package com.ninjaone.dundie_awards.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;

/** Record representing employee information */
@Builder(toBuilder = true)
public record EmployeeInfo (
    Long id,
    @NotBlank @NonNull String firstName,
    @NotBlank @NonNull String lastName,
    @Valid @NotNull @NonNull OrganizationInfo organization,
    @NotNull @NonNull Integer dundieAwards) {

    // Handle null dundieAwards by setting it to 0
    public static EmployeeInfoBuilder builder() {
        return new EmployeeInfoBuilder().dundieAwards(0);
    }

    public static class EmployeeInfoBuilder {
      // Handle null dundieAwards by setting it to 0
      public EmployeeInfoBuilder dundieAwards(Integer dundieAwards) {
          if (dundieAwards == null) {
              dundieAwards = 0;
          }
          this.dundieAwards = dundieAwards;
          return this;
      }
    }
  }

