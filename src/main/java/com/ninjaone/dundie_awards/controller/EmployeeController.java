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

    // get all employees
    @GetMapping("/employees")
    @ResponseBody
    public List<EmployeeInfo> getAllEmployees(int page, int size, String sortBy) throws InvalidArgumentException {
        // Return list of all employees
        return employeeService.findAll(page, size, sortBy).getContent();
    }

    // create employee rest api
    @PostMapping("/employees")
    @ResponseBody
    public EmployeeInfo createEmployee(@Valid @RequestBody EmployeeInfo employee) throws LookupException, InvalidArgumentException {
        // Save and return the new employee
        return employeeService.save(employee);
    }

    // get employee by id rest api
    @GetMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeInfo> getEmployeeById(@PathVariable Long id) throws LookupException, InvalidArgumentException {
        // get employee info by id
        EmployeeInfo employeeInfo = employeeService.getEmployeeInfoById(id);
        // return response entity, runtime exception will be thrown if not found
        return ResponseEntity.ok(employeeInfo);
    }

    // update employee rest api
    @PutMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeInfo> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeInfo employeeDetails) throws LookupException, InvalidArgumentException {
        // update employee and get updated info
        EmployeeInfo updatedEmployee = employeeService.update(id, employeeDetails);
        // return response entity, runtime exception will be thrown if not found
        return ResponseEntity.ok(updatedEmployee);
    }

    // delete employee rest api
    @DeleteMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) throws LookupException, InvalidArgumentException {
        // delete employee by id
        employeeService.delete(id);
        // return response that delete succeeded, runtime exception will be thrown if not successful
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // give dundie awards to every employee in an organization
    @PostMapping("/give-dundie-awards/{organizationId}")
    @ResponseBody
    public ResponseEntity<OrganizationInfo> giveDundieAwards(@PathVariable Long organizationId) throws LookupException, InvalidArgumentException {
        // increment dundie awards for all employees in the organization
        employeeService.incrementDundieAwardsForAll(organizationId);
        // return response entity, runtime exception will be thrown if not found
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // get total dundie awards for an organization
    @GetMapping("/get-dundie-awards/{organizationId}")
    @ResponseBody
    public ResponseEntity<Long> getDundieAwards(@PathVariable Long organizationId) throws LookupException, InvalidArgumentException {
        // get total dundie awards by organization id
        Long totalAwards = employeeService.getTotalAwardsByOrganization(organizationId);
        // return response entity, runtime exception will be thrown if not found
        return ResponseEntity.ok(totalAwards);
    }

    // get total dundie awards
    @GetMapping("/get-dundie-awards")
    @ResponseBody
    public ResponseEntity<Long> getTotalDundieAwards() throws LookupException, InvalidArgumentException {
        // get total dundie awards for all organizations
        return ResponseEntity.ok(employeeService.getTotalAwards());
    }
}
