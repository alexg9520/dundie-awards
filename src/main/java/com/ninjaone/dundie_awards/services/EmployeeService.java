package com.ninjaone.dundie_awards.services;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.ActivityInfo;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/** Service class for managing employees */
@Slf4j
@Component
public class EmployeeService extends AbstractDundieService {

    private static final String ACTIVITY_DUNDIE_AWARDS_INCREMENTED = "DUNDIE_AWARDS_INCREMENTED";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StreamBridge streamBridge;

    @Value("${DUNDIE_SPRING_CLOUD_STREAM_BINDING_OUT}")
    private String activityBindingName;

    // TODO: Use transactional template for complex transactions
    // public EmployeeService(TransactionalTemplate transactionalTemplate) {
    // }

    /**
     * Get EmployeeInfo by id
     * 
     * @param id the id of the employee
     * @return EmployeeInfo record or throws a runtime exception if not found
     */
    public EmployeeInfo getEmployeeInfoById(Long id) throws LookupException, InvalidArgumentException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        Employee employee = getEmployeeData(id);
        return createEmployeeInfoFromEmployeeNoCheck(employee);
    }

    /**
     * Find all employees
     * 
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @return Page<EmployeeInfo> of all employees
     */
    public Page<EmployeeInfo> findAll(int page, int size, String sortBy) throws InvalidArgumentException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return findAll(pageable);
    }

    /**
     * Find all employees
     * 
     * @param pageable the pagination information
     * @return Page<EmployeeInfo> of all employees
     * @throws InvalidArgumentException
     */
    public Page<EmployeeInfo> findAll(Pageable pageable) throws InvalidArgumentException {
        checkForNullValue(pageable, "Pageable is null", "No pagination information provided");
        Page<Employee> employees = employeeRepository.findAll(pageable);
        return employees.map(this::createEmployeeInfoFromEmployeeNoCheck);
    }    

    /**
     * Save a new employee
     * 
     * @param employee the EmployeeInfo to save
     * @return the saved Employee entity
     */
    public EmployeeInfo save(EmployeeInfo employee) throws LookupException, InvalidArgumentException {
        checkForNullValue(employee, "EmployeeInfo is null", "No employee information was provided");
        Organization organizationData = organizationService.getOrganizationData(employee.organization().id());
        Employee newEmployeeData = new Employee(employee.firstName(), employee.lastName(), organizationData);
        Employee employeeSaveData = saveData(newEmployeeData);
        return createEmployeeInfoFromEmployeeNoCheck(employeeSaveData);
    }

    /**
     * Save a new employee
     * 
     * @param employee the EmployeeInfo to save
     * @return the saved Employee entity
     */
    @Transactional
    private Employee saveData(Employee newEmployeeData) throws LookupException, InvalidArgumentException {
        return employeeRepository.save(newEmployeeData);
    }

    /**
     * Get total number of employees
     * 
     * @return the total number of employees
     */
    public long getEmployeeCount() {
        return employeeRepository.count();
    }

    /**
     * Get total number of employees in an organization
     * 
     * @param organizationId the id of the organization
     * @return the total number of employees in the organization
     */
    public long getEmployeeCount(Long organizationId) {
        return employeeRepository.countEmployeesByOrganization(organizationId);
    }
    
    private Employee getEmployeeData(Long employeeId) throws LookupException {
        Optional<Employee> existingEmployee = employeeRepository.findById(employeeId);
        if (!existingEmployee.isPresent()) {
            LookupException lookupException = new LookupException("The employee was not found");
            log.error("Employee not found with ID: {}", employeeId, lookupException);
            throw lookupException;
        }
        return existingEmployee.get();
    }

    /**
     * Update employee by id
     * 
     * @param id the id of the employee to update
     * @param employeeInfo the new employee details
     * @return EmployeeInfo record of updated employee or throws a runtime exception if not found
     */
    @Transactional
    public EmployeeInfo update(Long id, EmployeeInfo employeeInfo) throws LookupException, InvalidArgumentException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        checkForNullValue(employeeInfo, "EmployeeInfo is null", "No employee information was provided");
        Employee employeeData = getEmployeeData(id);

        // Only allow updates to first and last name and organization
        employeeData.setFirstName(employeeInfo.firstName());
        employeeData.setLastName(employeeInfo.lastName());
        if (employeeInfo.organization() != null) {
            Organization organization = organizationService.getOrganizationData(employeeInfo.organization().id());
            employeeData.setOrganization(organization);
        }

        // Save updated employee
        Employee updatedEmployee = employeeRepository.save(employeeData);

        // Check to see if organization has changed
        if (employeeInfo.organization().id() != employeeData.getOrganization().getId()) {
            Organization organization = organizationService.getOrganizationData(employeeInfo.organization().id());
            employeeData.setOrganization(organization);
            
            // Evict caches for organization since dundie awards totals may have changed
            evictTotalAwardsCache(employeeData.getOrganization().getId());
            evictTotalAwardsCache(employeeInfo.organization().id());
        }

        return createEmployeeInfoFromEmployeeNoCheck(updatedEmployee);
    }

    /**
     * Delete employee by id
     * 
     * @param id the id of the employee to delete
     * @return EmployeeInfo of deleted employee or throws a runtime exception if not found
     */
    @Transactional
    public EmployeeInfo delete(Long id) throws LookupException, InvalidArgumentException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        Employee employee = getEmployeeData(id);
        EmployeeInfo employeeInfo = createEmployeeInfoFromEmployeeNoCheck(employee);
        employeeRepository.delete(employee);

        // Evict caches since dundie awards totals may have changed
        evictCachesAfterUpdate(employee.getOrganization().getId());

        return employeeInfo;
    }

    /**
     * Increment dundie awards for all employees in an organization
     * The update to the database and the sending of the activity event are done transactionally
     * 
     * @param organizationId the id of the organization
     * @return the number of employees updated
     */
    @Transactional
    public Long incrementDundieAwardsForAll(Long organizationId) throws LookupException, InvalidArgumentException {
        checkForNullValue(organizationId, "Organization ID is null", "No organization ID was provided");
        // Validate organization exists
        Organization organization = organizationService.getOrganizationData(organizationId);
        long added = employeeRepository.incrementDundieAwardsForAll(organization.getId());
        
        // Evict caches
        evictCachesAfterUpdate(organization.getId());

        // Send activity event to message broker
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
    public Long getTotalAwardsByOrganization(Long organizationId) throws LookupException, InvalidArgumentException {
        checkForNullValue(organizationId, "Organization ID is null", "No organization ID was provided");
        // Validate organization exists
        Organization organization = organizationService.getOrganizationData(organizationId);
        return employeeRepository.getTotalAwardsByOrganization(organization.getId()).orElse(0L);
    }

    /**
     * Get total dundie awards
     * 
     * @return the total number of dundie awards for all organizations
     */
    @Cacheable(value="totalAwards")
    public Long getTotalAwards() throws LookupException, InvalidArgumentException {
        return employeeRepository.getTotalAwards().orElse(0L);
    }

    /** Evict caches after updates to dundie awards
     * 
     * @param organizationId the id of the organization
     */
    private void evictCachesAfterUpdate(Long organizationId) {
        evictTotalAwardsCache();
        evictTotalAwardsCache(organizationId);
    }

    /** Evict total awards cache */
    @CacheEvict(value="totalAwards")
    private void evictTotalAwardsCache() {
        log.debug("Total awards cache evicted");
    }

    /** Evict total awards by organization cache
     * 
     * @param organizationId the id of the organization
     */
    @CacheEvict(value="totalAwards", key="#organizationId")
    private void evictTotalAwardsCache(long organizationId) {
        log.debug("Total awards cache evicted for organizationId: {}", organizationId);
    }

}
