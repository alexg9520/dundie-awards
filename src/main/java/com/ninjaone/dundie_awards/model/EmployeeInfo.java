package com.ninjaone.dundie_awards.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;

/** 
 * Record representing employee information 
 * 
 * id - the unique identifier of the employee. It is initialized to 0 if null, and should be generated when saved
 * firstName - the first name of the employee
 * lastName - the last name of the employee
 * organization - the organization the employee belongs to
 * dundieAwards - the number of Dundie Awards the employee has received
 * */
@Builder(toBuilder = true)
public record EmployeeInfo (
    @NotNull Long id,
    @NotBlank @NonNull String firstName,
    @NotBlank @NonNull String lastName,
    @Valid @NotNull @NonNull OrganizationInfo organization,
    @NotNull @NonNull Integer dundieAwards) {

    public EmployeeInfo {
        if (id == null) {
            id = 0L;
        }

        if (organization.id() == null) {
            organization = organization.toBuilder().id(0L).build();
        }
    }

    // Handle null dundieAwards by setting it to 0, and Id defaults to 0
    public static EmployeeInfoBuilder builder() {
        return new EmployeeInfoBuilder().dundieAwards(0).id(0L);
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

