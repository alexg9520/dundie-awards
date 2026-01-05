package com.ninjaone.dundie_awards.services;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDundieService {

    /**
     * Create EmployeeInfo from Employee entity
     * 
     * @param employee the Employee entity
     * @return EmployeeInfo record
     */
    protected EmployeeInfo createEmployeeInfoFromEmployee(Employee employee) throws InvalidArgumentException {
        checkForNullValue(employee, "Employee is null", "No employee was provided");
        return createEmployeeInfoFromEmployeeNoCheck(employee);
    }

    /**
     * Create EmployeeInfo from Employee entity without null check
     * 
     * @param employee the Employee entity
     * @return EmployeeInfo record
     */
    protected EmployeeInfo createEmployeeInfoFromEmployeeNoCheck(Employee employee) {
        // Create nested OrganizationInfo from Employee's Organization
        EmployeeInfo employeeInfo = EmployeeInfo.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .dundieAwards(employee.getDundieAwards())
                .organization(createOrganizationInfoFromOrganizationNoCheck(employee.getOrganization()))
                .build();
        return employeeInfo;
    }

    protected Employee createEmployeeFromEmployeeInfo(EmployeeInfo employeeInfo) throws InvalidArgumentException {
        Employee newEmployeeData = new Employee(employeeInfo.firstName(), employeeInfo.lastName(), createOrganizationFromOrganizationInfo(employeeInfo.organization()));
        newEmployeeData.setId(employeeInfo.id());
        newEmployeeData.setDundieAwards(employeeInfo.dundieAwards());
        return newEmployeeData;
    }

    /**
     * Create OrganizationInfo from Organization entity
     * 
     * @param organization the Organization entity
     * @return OrganizationInfo record
     */
    protected OrganizationInfo createOrganizationInfoFromOrganization(Organization organization) throws InvalidArgumentException {
        checkForNullValue(organization, "Organization is null", "No organization was provided");
        return createOrganizationInfoFromOrganizationNoCheck(organization);
    }

    /**
     * Create OrganizationInfo from Organization entity
     * 
     * @param organization the Organization entity
     * @return OrganizationInfo record
     */
    protected OrganizationInfo createOrganizationInfoFromOrganizationNoCheck(Organization organization) {
        OrganizationInfo organizationInfo = OrganizationInfo.builder()
                .id(organization.getId())
                .name(organization.getName())
                .build();
        return organizationInfo;
    }

    protected Organization createOrganizationFromOrganizationInfo(OrganizationInfo organizationInfo) throws InvalidArgumentException {
        Organization newOrganizationData = new Organization(organizationInfo.name());
        newOrganizationData.setId(organizationInfo.id());
        return newOrganizationData;
    }

    protected void checkForNullValue(Object obj, String logMessage, String errorMessage) throws InvalidArgumentException {
        if (obj == null) {
            log.error(logMessage);
            throw new InvalidArgumentException(errorMessage);
        }
    }
}
