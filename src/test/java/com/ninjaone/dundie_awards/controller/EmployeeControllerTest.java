package com.ninjaone.dundie_awards.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.services.EmployeeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Controller Tests")
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private OrganizationInfo testOrganization;
    
    private EmployeeInfo testEmployee;

    @BeforeEach
    void setUp() {
        testOrganization = OrganizationInfo.builder()
                .id(1L)
                .name("Dunder Mifflin")
                .build();

        testEmployee = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scott")
                .organization(testOrganization)
                .dundieAwards(5)
                .build();

    }

    @Test
    @DisplayName("GET /employees - should return all employees")
    void getAllEmployees_ShouldReturnListOfEmployees() throws InvalidArgumentException {
        // Arrange
        EmployeeInfo employee2 = EmployeeInfo.builder()
                .id(2L)
                .firstName("Jim")
                .lastName("Halpert")
                .organization(testOrganization)
                .dundieAwards(3)
                .build();

        List<EmployeeInfo> employees = Arrays.asList(testEmployee, employee2);
        Page<EmployeeInfo> employeePage = new PageImpl<>(employees, PageRequest.of(0, 100), employees.size());
        when(employeeService.findAll(0, 100, "id")).thenReturn(employeePage);

        // Act
        List<EmployeeInfo> result = employeeController.getAllEmployees(0, 100, "id");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("Michael");
        assertThat(result.get(1).firstName()).isEqualTo("Jim");
        verify(employeeService, times(1)).findAll(0, 100, "id");
    }

    @Test
    @DisplayName("GET /employees/{id} - should return employee when found")
    void getEmployeeById_WhenEmployeeExists_ShouldReturnEmployee() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.getEmployeeInfoById(1L)).thenReturn(testEmployee);

        // Act
        ResponseEntity<EmployeeInfo> response = employeeController.getEmployeeById(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().firstName()).isEqualTo("Michael");
        assertThat(response.getBody().lastName()).isEqualTo("Scott");
        verify(employeeService, times(1)).getEmployeeInfoById(1L);
    }

    @Test
    @DisplayName("GET /employees/{id} - should return InvalidArgumentException when employee not found")
    void getEmployeeById_WhenEmployeeDoesNotExist_ShouldReturnInvalidArgumentException() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.getEmployeeInfoById(999L)).thenThrow(new InvalidArgumentException("Employee not found"));

        try {
            // Act
            employeeController.getEmployeeById(999L);
        } catch (InvalidArgumentException ex) {
            // Assert
            assertThat(ex.getMessage()).isEqualTo("Employee not found");
        }
        // Assert
        verify(employeeService, times(1)).getEmployeeInfoById(999L);
    }

    @Test
    @DisplayName("POST /employees - should create new employee")
    void createEmployee_WithValidData_ShouldReturnCreatedEmployee() throws LookupException, InvalidArgumentException {
        // Arrange
        EmployeeInfo newEmployee = EmployeeInfo.builder()
                .firstName("Pam")
                .lastName("Beesly")
                .organization(testOrganization)
                .dundieAwards(0)
                .build();

        when(employeeService.save(any(EmployeeInfo.class))).thenReturn(testEmployee);

        // Act
        EmployeeInfo result = employeeController.createEmployee(newEmployee);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Michael");
        verify(employeeService, times(1)).save(any(EmployeeInfo.class));
    }

    @Test
    @DisplayName("PUT /employees/{id} - should update existing employee")
    void updateEmployee_WhenEmployeeExists_ShouldReturnUpdatedEmployee() throws LookupException, InvalidArgumentException {
        // Arrange
        EmployeeInfo updatedEmployee = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scarn")
                .organization(testOrganization)
                .dundieAwards(5)
                .build();

        when(employeeService.update(eq(1L), any(EmployeeInfo.class)))
                .thenReturn(updatedEmployee);

        // Act
        ResponseEntity<EmployeeInfo> response = employeeController.updateEmployee(1L, updatedEmployee);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().lastName()).isEqualTo("Scarn");
        verify(employeeService, times(1)).update(eq(1L), any(EmployeeInfo.class));
    }

    @Test
    @DisplayName("PUT /employees/{id} - should return 404 when employee not found")
    void updateEmployee_WhenEmployeeDoesNotExist_ShouldReturnInvalidArgumentException() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.update(eq(999L), any(EmployeeInfo.class)))
                .thenThrow(new InvalidArgumentException("Employee not found"));

        try {
            // Act
            employeeController.updateEmployee(999L, testEmployee);
        } catch (InvalidArgumentException ex) {
            // Assert
            assertThat(ex.getMessage()).isEqualTo("Employee not found");
        }
        verify(employeeService, times(1)).update(eq(999L), any(EmployeeInfo.class));
    }

    @Test
    @DisplayName("DELETE /employees/{id} - should delete employee successfully")
    void deleteEmployee_WhenEmployeeExists_ShouldReturnSuccessResponse() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.delete(1L)).thenReturn(testEmployee);

        // Act
        ResponseEntity<Map<String, Boolean>> response = employeeController.deleteEmployee(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("deleted")).isTrue();
        verify(employeeService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("DELETE /employees/{id} - should return 404 when employee not found")
    void deleteEmployee_WhenEmployeeDoesNotExist_ShouldReturnInvalidArgumentException() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.delete(999L)).thenThrow(new InvalidArgumentException("Employee not found"));

        try {
            // Act
            employeeController.deleteEmployee(999L);
        } catch (InvalidArgumentException ex) {
            // Assert
            assertThat(ex.getMessage()).isEqualTo("Employee not found");
        }

        // Assert
        verify(employeeService, times(1)).delete(999L);
    }

    @Test
    @DisplayName("DELETE /employees/{id} - should return 404 when id is null")
    void deleteEmployee_WhenIdIsNull_ShouldReturnInvalidArgumentException() throws LookupException, InvalidArgumentException {

        try {
            // Act
            employeeController.deleteEmployee(null);
        } catch (InvalidArgumentException ex) {
            // Assert
            assertThat(ex.getMessage()).isEqualTo("Employee ID cannot be null");
        }

        // Assert
        verify(employeeService, times(1)).delete(any());
    }

    @Test
    @DisplayName("POST /give-dundie-awards/{organizationId} - should increment awards successfully")
    void giveDundieAwards_WhenOrganizationExists_ShouldReturnOk() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.incrementDundieAwardsForAll(1L)).thenReturn(5L);

        // Act
        ResponseEntity<OrganizationInfo> response = employeeController.giveDundieAwards(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(employeeService, times(1)).incrementDundieAwardsForAll(1L);
    }

    @Test
    @DisplayName("POST /give-dundie-awards/{organizationId} - should return 404 when no employees updated")
    void giveDundieAwards_WhenNoEmployeesUpdated_ShouldReturnLookupException() throws InvalidArgumentException, LookupException {
        // Arrange
        when(employeeService.incrementDundieAwardsForAll(1L)).thenThrow(new LookupException("No organization found"));

        try {
            // Act
            employeeController.giveDundieAwards(1L);
        } catch (LookupException ex) {
            // Assert
            assertThat(ex.getMessage()).isEqualTo("No organization found");
        }

        // Assert
        verify(employeeService, times(1)).incrementDundieAwardsForAll(1L);
    }

    @Test
    @DisplayName("POST /give-dundie-awards/{organizationId} - should return 404 when organization id is null")
    void giveDundieAwards_WhenOrganizationIdIsNull_ShouldReturnInvalidArgumentException() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeService.incrementDundieAwardsForAll(null))
                .thenThrow(new InvalidArgumentException("Organization ID cannot be null"));

        try {
            // Act
            employeeController.giveDundieAwards(null);
        } catch (InvalidArgumentException ex) {
            // Assert
            assertThat(ex.getMessage()).isEqualTo("Organization ID cannot be null");
        }

        // Assert
        verify(employeeService, times(1)).incrementDundieAwardsForAll(null);
    }
}
