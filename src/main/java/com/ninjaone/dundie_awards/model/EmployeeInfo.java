package com.ninjaone.dundie_awards.model;

import lombok.Builder;
import lombok.NonNull;

/** Record representing employee information */
@Builder(toBuilder = true)
public record EmployeeInfo (
    Long id,
    @NonNull String firstName,
    @NonNull String lastName,
    @NonNull OrganizationInfo organization,
    @NonNull Integer dundieAwards) {
          
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
