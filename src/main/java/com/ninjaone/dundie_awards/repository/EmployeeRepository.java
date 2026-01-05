package com.ninjaone.dundie_awards.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ninjaone.dundie_awards.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Count employees by organization id
     * @param organizationId the id of the organization
     * @return the number of employees in the organization
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.organization.id = :organizationId")
    long countEmployeesByOrganization(Long organizationId);

    /**
     * Increment dundie awards for all employees in an organization
     * @param organizationId the id of the organization
     * @return the number of employees updated
     */
    @Modifying
    @Query("UPDATE Employee e SET e.dundieAwards = COALESCE(e.dundieAwards, 0) + 1 WHERE e.organization.id = :organizationId")
    Long incrementDundieAwardsForAll(Long organizationId);

    /**
     * Get total dundie awards for an organization
     * @param organizationId the id of the organization
     * @return the total number of dundie awards for the organization
     */
    @Query("SELECT SUM(COALESCE(dundieAwards, 0)) FROM Employee e WHERE e.organization.id = :organizationId")
    Optional<Long> getTotalAwardsByOrganization(Long organizationId);

    /**
     * Get total dundie awards
     * @return the total number of dundie awards for all organizations
     */
    @Query("SELECT SUM(COALESCE(dundieAwards, 0)) FROM Employee")
    Optional<Long> getTotalAwards();

    /**
     * Delete all employees in an organization
     * @param organizationId the id of the organization
     * @return the number of employees deleted
     */
    @Modifying
    @Query("DELETE FROM Employee e WHERE e.organization.id = :organizationId")
    Long deleteAllByOrganizationId(Long organizationId);
}

