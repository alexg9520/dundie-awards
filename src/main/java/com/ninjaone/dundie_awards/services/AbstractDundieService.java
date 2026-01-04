package com.ninjaone.dundie_awards.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDundieService {
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException  ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            FieldError field = ((FieldError) error);
            errors.put(field.getField(), field.getDefaultMessage());
        });
        return errors;
    }

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

    protected void checkForNullValue(Object obj, String logMessage, String errorMessage) throws InvalidArgumentException {
        if (obj == null) {
            log.error(logMessage);
            throw new InvalidArgumentException(errorMessage);
        }
    }
}
