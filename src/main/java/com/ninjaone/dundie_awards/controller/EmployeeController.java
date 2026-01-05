package com.ninjaone.dundie_awards.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.services.EmployeeService;

import jakarta.validation.Valid;

// TODO: Needs security
@Controller
@RequestMapping()
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * Get all employees with pagination
     * 
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @return List of EmployeeInfo
     * @throws InvalidArgumentException if pagination parameters are invalid
     */
    @GetMapping("/employees")
    @ResponseBody
    public List<EmployeeInfo> getAllEmployees(int page, int size, String sortBy) throws InvalidArgumentException {
        return employeeService.findAll(page, size, sortBy).getContent();
    }

    /**
     * Create a new employee
     * 
     * @param employee the employee information, id defaults to 0, as it will be generated, and dundieAwards will be set to 0
     * @return the created EmployeeInfo
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if employee data is invalid
     */
    @PostMapping("/employees")
    @ResponseBody
    public EmployeeInfo createEmployee(@Valid @RequestBody EmployeeInfo employee) throws LookupException, InvalidArgumentException {
        return employeeService.save(employee);
    }

    /**
     * Get employee by id
     * 
     * @param id the employee id
     * @return ResponseEntity containing the EmployeeInfo
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if id is invalid
     */
    @GetMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeInfo> getEmployeeById(@PathVariable Long id) throws LookupException, InvalidArgumentException {
        EmployeeInfo employeeInfo = employeeService.getEmployeeInfoById(id);
        return ResponseEntity.ok(employeeInfo);
    }

    /**
     * Update an existing employee
     * 
     * @param id the employee id
     * @param employeeDetails the updated employee information
     * @return ResponseEntity containing the updated EmployeeInfo
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if data is invalid
     */
    @PutMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeInfo> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeInfo employeeDetails) throws LookupException, InvalidArgumentException {
        EmployeeInfo updatedEmployee = employeeService.update(id, employeeDetails);
        return ResponseEntity.ok(updatedEmployee);
    }

    /**
     * Delete an employee
     * 
     * @param id the employee id
     * @return ResponseEntity with deletion confirmation
     * @throws LookupException if employee is not found
     * @throws InvalidArgumentException if id is invalid
     */
    @DeleteMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) throws LookupException, InvalidArgumentException {
        employeeService.delete(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    /**
     * Give dundie awards to every employee in an organization
     * 
     * @param organizationId the organization id
     * @return ResponseEntity with OK status
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if organizationId is invalid
     */
    @PostMapping("/give-dundie-awards/{organizationId}")
    @ResponseBody
    public ResponseEntity<OrganizationInfo> giveDundieAwards(@PathVariable Long organizationId) throws LookupException, InvalidArgumentException {
        employeeService.incrementDundieAwardsForAll(organizationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Get total dundie awards for an organization
     * 
     * @param organizationId the organization id
     * @return ResponseEntity containing the total awards count
     * @throws LookupException if organization is not found
     * @throws InvalidArgumentException if organizationId is invalid
     */
    @GetMapping("/get-dundie-awards/{organizationId}")
    @ResponseBody
    public ResponseEntity<Long> getDundieAwards(@PathVariable Long organizationId) throws LookupException, InvalidArgumentException {
        Long totalAwards = employeeService.getTotalAwardsByOrganization(organizationId);
        return ResponseEntity.ok(totalAwards);
    }

    /**
     * Get total dundie awards across all organizations
     * 
     * @return ResponseEntity containing the total awards count
     * @throws LookupException if an error occurs during retrieval
     * @throws InvalidArgumentException if validation fails
     */
    @GetMapping("/get-dundie-awards")
    @ResponseBody
    public ResponseEntity<Long> getTotalDundieAwards() throws LookupException, InvalidArgumentException {
        return ResponseEntity.ok(employeeService.getTotalAwards());
    }
}
