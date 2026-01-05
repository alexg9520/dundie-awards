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

import com.ninjaone.dundie_awards.cache.CacheConfig;
import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.ActivityInfo;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/** Service class for managing employees */
@Slf4j
@Component
public class EmployeeService extends AbstractDundieService {

    private static final String CACHE_ORGANIZATION_ID = "#organizationId";

    private static final String ACTIVITY_DUNDIE_AWARDS_INCREMENTED = "DUNDIE_AWARDS_INCREMENTED";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StreamBridge streamBridge;

    @Value("${DUNDIE_SPRING_CLOUD_STREAM_BINDING_OUT}")
    private String activityBindingName;

    /**
     * Get EmployeeInfo by id
     * 
     * @param id the id of the employee
     * @return EmployeeInfo record
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if id is null
     */
    public EmployeeInfo getEmployeeInfoById(Long id) throws LookupException, InvalidArgumentException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");
        Employee employee = getEmployeeData(id);
        return createEmployeeInfoFromEmployeeNoCheck(employee);
    }

    /**
     * Find all employees with pagination
     * 
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @return Page<EmployeeInfo> of all employees
     * @throws InvalidArgumentException if pagination parameters are null
     */
    public Page<EmployeeInfo> findAll(int page, int size, String sortBy) throws InvalidArgumentException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return findAll(pageable);
    }

    /**
     * Find all employees with pagination
     * 
     * @param pageable the pagination information
     * @return Page<EmployeeInfo> of all employees
     * @throws InvalidArgumentException if pageable is null
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
     * @return the saved EmployeeInfo record
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if employee data is null or invalid
     */
    public EmployeeInfo save(EmployeeInfo employeeInfo) throws LookupException, InvalidArgumentException {
        checkForNullValue(employeeInfo, "EmployeeInfo is null", "No employee information was provided");

        // Validate organization exists
        OrganizationInfo organizationInfo = organizationService.getOrganizationInfo(employeeInfo.organization().id());

        // Only first name, last name, and organization are allowed when creating a new employee
        Employee newEmployeeData = new Employee(employeeInfo.firstName(), employeeInfo.lastName(), createOrganizationFromOrganizationInfo(organizationInfo));
        Employee employeeSaveData = saveData(newEmployeeData);
        return createEmployeeInfoFromEmployeeNoCheck(employeeSaveData);
    }

    /**
     * Save employee data to the repository
     * 
     * @param newEmployeeData the Employee entity to save
     * @return the saved Employee entity
     * @throws LookupException if an error occurs during save
     * @throws InvalidArgumentException if employee data is invalid
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

    /**
     * Get EmployeeInfo by id
     * 
     * @param employeeId the id of the employee
     * @return EmployeeInfo record
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if employeeId is null
     */
    public EmployeeInfo getEmployeeInfo(Long employeeId) throws LookupException, InvalidArgumentException {
        checkForNullValue(employeeId, "Employee ID is null", "No employee ID was provided");
        return createEmployeeInfoFromEmployeeNoCheck(getEmployeeData(employeeId));
    }

    /**
     * Get employee entity by id
     * 
     * @param employeeId the id of the employee
     * @return Employee entity
     * @throws LookupException if employee is not found
     */
    private Employee getEmployeeData(Long employeeId) throws LookupException {
        // Get employee data
        Optional<Employee> existingEmployee = employeeRepository.findById(employeeId);
        if (!existingEmployee.isPresent()) {
            // Employee not found
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
     * @return EmployeeInfo record of updated employee
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if id or employeeInfo is null
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
            OrganizationInfo organizationInfo = organizationService.getOrganizationInfo(employeeInfo.organization().id());
            employeeData.setOrganization(createOrganizationFromOrganizationInfo(organizationInfo));
        }

        // Save updated employee
        Employee updatedEmployee = employeeRepository.save(employeeData);

        // Check to see if organization has changed
        if (employeeInfo.organization().id() != employeeData.getOrganization().getId()) {
            OrganizationInfo organizationInfo = organizationService.getOrganizationInfo(employeeInfo.organization().id());
            employeeData.setOrganization(createOrganizationFromOrganizationInfo(organizationInfo));
            
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
     * @return EmployeeInfo of deleted employee
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if id is null
     */
    @Transactional
    public EmployeeInfo delete(Long id) throws LookupException, InvalidArgumentException {
        checkForNullValue(id, "Employee ID is null", "No employee ID was provided");

        // Get employee data
        Employee employee = getEmployeeData(id);
        EmployeeInfo employeeInfo = createEmployeeInfoFromEmployeeNoCheck(employee);

        // Delete employee
        employeeRepository.delete(employee);

        // Evict caches since dundie awards totals may have changed
        evictCachesAfterUpdate(employee.getOrganization().getId());

        return employeeInfo;
    }

    @Transactional
    public OrganizationInfo deleteEmployeesAndOrganization(Long organizationId) throws LookupException, InvalidArgumentException {
        // Validate organization exists
        OrganizationInfo organizationInfo = organizationService.getOrganizationInfo(organizationId);

        // Delete all employees in the organization
        employeeRepository.deleteAllByOrganizationId(organizationId);
        organizationService.delete(organizationId);

        // Evict caches since dundie awards totals may have changed
        evictCachesAfterUpdate(organizationInfo.id());
        return organizationInfo;
    }

        /**
     * Increment dundie awards for all employees in an organization
     * The update to the database and the sending of the activity event are done transactionally
     * 
     * @param organizationId the id of the organization
     * @return the number of employees updated
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if organizationId is null
     */
    @Transactional
    public Long incrementDundieAwardsForAll(Long organizationId) throws LookupException, InvalidArgumentException {
        checkForNullValue(organizationId, "Organization ID is null", "No organization ID was provided");

        // Validate organization exists
        OrganizationInfo organizationInfo = organizationService.getOrganizationInfo(organizationId);
        long added = employeeRepository.incrementDundieAwardsForAll(organizationInfo.id());
        
        // Evict caches
        evictCachesAfterUpdate(organizationInfo.id());

        // Send activity event to message broker
        streamBridge.send(activityBindingName, ActivityInfo.builder().occuredAt(LocalDateTime.now()).event(ACTIVITY_DUNDIE_AWARDS_INCREMENTED + ": " + added).build());
        return added;
    }
    
    /**
     * Get total dundie awards for an organization
     * 
     * @param organizationId the id of the organization
     * @return the total number of dundie awards for the organization
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if organizationId is null
     */
    @Cacheable(cacheNames = CacheConfig.TOTAL_AWARDS_BY_ORG_CACHE, key = CACHE_ORGANIZATION_ID)
    public Long getTotalAwardsByOrganization(Long organizationId) throws LookupException, InvalidArgumentException {
        checkForNullValue(organizationId, "Organization ID is null", "No organization ID was provided");

        // Validate organization exists
        OrganizationInfo organizationInfo = organizationService.getOrganizationInfo(organizationId);
        return employeeRepository.getTotalAwardsByOrganization(organizationInfo.id()).orElse(0L);
    }

    /**
     * Get total dundie awards across all organizations
     * 
     * @return the total number of dundie awards for all organizations
     * @throws LookupException if an error occurs during retrieval
     * @throws InvalidArgumentException if validation fails
     */
    @Cacheable(cacheNames = CacheConfig.TOTAL_AWARDS_CACHE)
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
    @CacheEvict(cacheNames = CacheConfig.TOTAL_AWARDS_CACHE)
    private void evictTotalAwardsCache() {
        log.debug("Total awards cache evicted");
    }

    /** Evict total awards by organization cache
     * 
     * @param organizationId the id of the organization
     */
    @CacheEvict(cacheNames = CacheConfig.TOTAL_AWARDS_BY_ORG_CACHE, key=CACHE_ORGANIZATION_ID)
    private void evictTotalAwardsCache(long organizationId) {
        log.debug("Total awards cache evicted for organizationId: {}", organizationId);
    }

}
