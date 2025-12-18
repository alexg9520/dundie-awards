package com.ninjaone.dundie_awards.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.services.EmployeeService;

// FIXME: Needs security
@Controller
@RequestMapping()
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // get all employees
    @GetMapping("/employees")
    @ResponseBody
    // TODO: needs to support pagination
    public List<EmployeeInfo> getAllEmployees() {
        return employeeService.findAll();
    }

    // create employee rest api
    @PostMapping("/employees")
    @ResponseBody
    // TODO: needs to support pagination
    public Employee createEmployee(@RequestBody EmployeeInfo employee) {
        return employeeService.save(employee);
    }

    // get employee by id rest api
    @GetMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeInfo> getEmployeeById(@PathVariable Long id) {
        Optional<EmployeeInfo> optionalEmployee = employeeService.getEmployeeInfoById(id);
        if (optionalEmployee.isPresent()) {
            return ResponseEntity.ok(optionalEmployee.get());
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // update employee rest api
    @PutMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeInfo> updateEmployee(@PathVariable Long id, @RequestBody EmployeeInfo employeeDetails) {
        Optional<EmployeeInfo> updatedEmployee = employeeService.update(id, employeeDetails);
        if (!updatedEmployee.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(updatedEmployee.get());
    }

    // delete employee rest api
    @DeleteMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) {
        if (id == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Optional<EmployeeInfo> deletedEmployee = employeeService.delete(id);
        if (!deletedEmployee.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // give dundie awards to every employee in an organization
    @PostMapping("/give-dundie-awards/{organizationId}")
    @ResponseBody
    public ResponseEntity<OrganizationInfo> giveDundieAwards(@PathVariable Long organizationId) {
        if (organizationId == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }        
        Long updatedCount = employeeService.incrementDundieAwardsForAll(organizationId);
        if (updatedCount > 0) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } 
    }

    // get total dundie awards for an organization
    @GetMapping("/get-dundie-awards/{organizationId}")
    @ResponseBody
    public ResponseEntity<Long> getDundieAwards(@PathVariable Long organizationId) {
        if (organizationId == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Long totalAwards = employeeService.getTotalAwardsByOrganization(organizationId);
        return ResponseEntity.ok(totalAwards);
    }

    // get total dundie awards
    @GetMapping("/get-dundie-awards")
    @ResponseBody
    public ResponseEntity<Long> getTotalDundieAwards() {
        Long totalAwards = employeeService.getTotalAwards();       
        return ResponseEntity.ok(totalAwards);
    }
}
