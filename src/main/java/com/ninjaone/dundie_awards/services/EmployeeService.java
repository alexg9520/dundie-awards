package com.ninjaone.dundie_awards.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.ActivityInfo;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/** Service class for managing employees */
@Slf4j
@Component
public class EmployeeService {

    private static final String ACTIVITY_DUNDIE_AWARDS_INCREMENTED = "DUNDIE_AWARDS_INCREMENTED";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private StreamBridge streamBridge;

    @Value("${spring.cloud.stream.binding.out}")
    private String activityBindingName;

    // TODO: Use transactional template for complex transactions
    // public EmployeeService(TransactionalTemplate transactionalTemplate) {
    // }

    /**
     * Get EmployeeInfo by id
     * 
     * @param id the id of the employee
     * @return Optional<EmployeeInfo> of the employee, or Optional.empty() if not found
     * @throws LookupException 
     */
    public Optional<EmployeeInfo> getEmployeeInfoById(Long id) throws LookupException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        Employee employee = getEmployeeData(id);
        return Optional.of(createEmployeeInfoFromEmployee(employee));
    }

    /**
     * Find all employees
     * 
     * @return List<EmployeeInfo> of all employees
     */
    // TODO: needs to support pagination
    public List<EmployeeInfo> findAll() {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream().map(this::createEmployeeInfoFromEmployee).toList();
    }

    /**
     * Save a new employee
     * 
     * @param employee the EmployeeInfo to save
     * @return the saved Employee entity
     */
    @Transactional
    public Employee save(EmployeeInfo employee) {
        checkForNullValue(employee, "EmployeeInfo is null", "No employee information was provided");
        // FIXME: Do I need this check?
        // if (employee.organizationId() == null) {
        //     log.error("Organization ID is null in EmployeeInfo: {}", employee);
        //     throw new InvalidArgumentException("No organization ID was provided");
        // }
        Organization organization = getOrganizationData(employee.organization().id());
        Employee newEmployee = new Employee(employee.firstName(), employee.lastName(), organization);
        return employeeRepository.save(newEmployee);
    }

    // TODO: Could use cache for organization lookup
    private Organization getOrganizationData(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId).orElseThrow(() -> {
            LookupException lookupException = new LookupException("The organization was not found");
            log.error("Invalid organization ID '{}'", organizationId, lookupException);
            throw lookupException;
        });
        return organization;
    }

    private Employee getEmployeeData(Long employeeId) {
        Employee existingEmployee = employeeRepository.findById(employeeId).orElseThrow(() -> {
            LookupException lookupException = new LookupException("The employee was not found");
            log.error("Employee not found with ID: {}", employeeId, lookupException);
            throw lookupException;
        });
        return existingEmployee;
    }

    private void checkForNullValue(Object obj, String logMessage, String errorMessage) {
        if (obj == null) {
            log.error(logMessage);
            throw new InvalidArgumentException(errorMessage);
        }
    }

    /**
     * Update employee by id
     * 
     * @param id the id of the employee to update
     * @param employeeInfo the new employee details
     * @return Optional<EmployeeInfo> of updated employee, or Optional.empty() if not found
     * @throws LookupException 
     */
    @Transactional
    public Optional<EmployeeInfo> update(Long id, EmployeeInfo employeeInfo) throws LookupException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        checkForNullValue(employeeInfo, "EmployeeInfo is null", "No employee information was provided");
        // FIXME: Do I need this check?
        // if (employeeDetails.organizationId() == null) {
        //     log.error("Organization ID is null in EmployeeInfo: {}", employeeDetails);
        //     throw new InvalidArgumentException("No organization ID was provided");
        // }
        Employee employeeData = getEmployeeData(id);

        // Only update mutable fields
        employeeData.setFirstName(employeeInfo.firstName());
        employeeData.setLastName(employeeInfo.lastName());
        if (employeeInfo.organization() != null) {
            // TODO: There may be a better way to handle organization updates
            Organization organization = getOrganizationData(employeeInfo.organization().id());
            employeeData.setOrganization(organization);
        }

        Employee updatedEmployee = employeeRepository.save(employeeData);
        return Optional.of(createEmployeeInfoFromEmployee(updatedEmployee));
    }
    
    /**
     * Create EmployeeInfo from Employee entity
     * 
     * @param employee the Employee entity
     * @return EmployeeInfo record
     */
    public EmployeeInfo createEmployeeInfoFromEmployee(Employee employee) {
        checkForNullValue(employee, "Employee is null", "No employee was provided");
        // FIXME: Do I need this check?
        // checkForNullValue(employee.getOrganization(), "Employee's organization is null", "The employee's organization was not provided");
        EmployeeInfo employeeInfo = EmployeeInfo.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .dundieAwards(employee.getDundieAwards())
                .organization(createOrganizationInfoFromOrganization(employee.getOrganization()))
                .build();
        return employeeInfo;
    }

    public OrganizationInfo createOrganizationInfoFromOrganization(Organization organization) {
        checkForNullValue(organization, "Organization is null", "No organization was provided");
        // FIXME: Do I need this check?
        // checkForNullValue(organization.getOrganization(), "Organization's organization is null", "The organization's organization was not provided");
        OrganizationInfo organizationInfo = OrganizationInfo.builder()
                .id(organization.getId())
                .name(organization.getName())
                .build();
        return organizationInfo;
    }    

    /**
     * Delete employee by id
     * 
     * @param id the id of the employee to delete
     * @return Optional<EmployeeInfo> of deleted employee, or Optional.empty() if not found
     */
    @Transactional
    public Optional<EmployeeInfo> delete(Long id) {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        Employee employee = getEmployeeData(id);
        EmployeeInfo employeeInfo = createEmployeeInfoFromEmployee(employee);
        employeeRepository.delete(employee);
        return Optional.of(employeeInfo);
    }

    /**
     * Increment dundie awards for all employees in an organization
     * 
     * @param organizationId the id of the organization
     * @return the number of employees updated
     */
    @Transactional
    public Long incrementDundieAwardsForAll(Long organizationId) {
        // Validate organization exists
        Organization organization = getOrganizationData(organizationId);
        long added = employeeRepository.incrementDundieAwardsForAll(organization.getId());
        
        // Evict caches
        evictCachesAfterUpdate(organization.getId());

        // Send activity event to save it asynchronously
        streamBridge.send(activityBindingName, ActivityInfo.builder().occuredAt(LocalDateTime.now()).event(ACTIVITY_DUNDIE_AWARDS_INCREMENTED + ": " + added).build());
        return added;
    }
    
    /**
     * Get total dundie awards for an organization
     * 
     * @param organizationId the id of the organization
     * @return the total number of dundie awards for the organization
     */
    @Cacheable(value="totalAwardsByOrganization", key="#organizationId")
    public Long getTotalAwardsByOrganization(Long organizationId) {
        // Validate organization exists
        Organization organization = getOrganizationData(organizationId);
        return employeeRepository.getTotalAwardsByOrganization(organization.getId()).orElse(0L);
    }

    /**
     * Get total dundie awards
     * 
     * @return the total number of dundie awards for all organizations
     */
    @Cacheable(value="totalAwards")
    public Long getTotalAwards() {
        return employeeRepository.getTotalAwards().orElse(0L);
    }

    /** Evict caches after updates to dundie awards
     * 
     * @param organizationId the id of the organization
     */
    private void evictCachesAfterUpdate(Long organizationId) {
        deleteTotalAwards();
        deleteTotalAwards(organizationId);
    }

    /** Evict total awards cache */
    @CacheEvict(value="totalAwards")
    private void deleteTotalAwards() {
        log.debug("Total awards cache evicted");
    }

    /** Evict total awards by organization cache
     * 
     * @param organizationId the id of the organization
     */
    @CacheEvict(value="totalAwards", key="#organizationId")
    private void deleteTotalAwards(long organizationId) {
        log.debug("Total awards cache evicted for organizationId: {}", organizationId);
    }
}
