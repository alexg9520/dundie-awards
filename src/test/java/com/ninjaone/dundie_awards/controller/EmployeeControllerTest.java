package com.ninjaone.dundie_awards.controller;

import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.services.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Controller Tests")
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private OrganizationInfo testOrganization;
    private EmployeeInfo testEmployee;
    private Employee testEmployeeEntity;

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

        Organization org = new Organization("Dunder Mifflin");
        testEmployeeEntity = new Employee("Michael", "Scott", org);
    }

    @Test
    @DisplayName("GET /employees - should return all employees")
    void getAllEmployees_ShouldReturnListOfEmployees() {
        // Arrange
        EmployeeInfo employee2 = EmployeeInfo.builder()
                .id(2L)
                .firstName("Jim")
                .lastName("Halpert")
                .organization(testOrganization)
                .dundieAwards(3)
                .build();

        List<EmployeeInfo> employees = Arrays.asList(testEmployee, employee2);
        when(employeeService.findAll()).thenReturn(employees);

        // Act
        List<EmployeeInfo> result = employeeController.getAllEmployees();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("Michael");
        assertThat(result.get(1).firstName()).isEqualTo("Jim");
        verify(employeeService, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /employees/{id} - should return employee when found")
    void getEmployeeById_WhenEmployeeExists_ShouldReturnEmployee() {
        // Arrange
        when(employeeService.getEmployeeInfoById(1L)).thenReturn(Optional.of(testEmployee));

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
    @DisplayName("GET /employees/{id} - should return 404 when employee not found")
    void getEmployeeById_WhenEmployeeDoesNotExist_ShouldReturn404() {
        // Arrange
        when(employeeService.getEmployeeInfoById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<EmployeeInfo> response = employeeController.getEmployeeById(999L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(employeeService, times(1)).getEmployeeInfoById(999L);
    }

    @Test
    @DisplayName("POST /employees - should create new employee")
    void createEmployee_WithValidData_ShouldReturnCreatedEmployee() {
        // Arrange
        EmployeeInfo newEmployee = EmployeeInfo.builder()
                .firstName("Pam")
                .lastName("Beesly")
                .organization(testOrganization)
                .dundieAwards(0)
                .build();

        when(employeeService.save(any(EmployeeInfo.class))).thenReturn(testEmployeeEntity);

        // Act
        Employee result = employeeController.createEmployee(newEmployee);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Michael");
        verify(employeeService, times(1)).save(any(EmployeeInfo.class));
    }

    @Test
    @DisplayName("PUT /employees/{id} - should update existing employee")
    void updateEmployee_WhenEmployeeExists_ShouldReturnUpdatedEmployee() {
        // Arrange
        EmployeeInfo updatedEmployee = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scarn")
                .organization(testOrganization)
                .dundieAwards(5)
                .build();

        when(employeeService.update(eq(1L), any(EmployeeInfo.class)))
                .thenReturn(Optional.of(updatedEmployee));

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
    void updateEmployee_WhenEmployeeDoesNotExist_ShouldReturn404() {
        // Arrange
        when(employeeService.update(eq(999L), any(EmployeeInfo.class)))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<EmployeeInfo> response = employeeController.updateEmployee(999L, testEmployee);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(employeeService, times(1)).update(eq(999L), any(EmployeeInfo.class));
    }

    @Test
    @DisplayName("DELETE /employees/{id} - should delete employee successfully")
    void deleteEmployee_WhenEmployeeExists_ShouldReturnSuccessResponse() {
        // Arrange
        when(employeeService.delete(1L)).thenReturn(Optional.of(testEmployee));

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
    void deleteEmployee_WhenEmployeeDoesNotExist_ShouldReturn404() {
        // Arrange
        when(employeeService.delete(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Boolean>> response = employeeController.deleteEmployee(999L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(employeeService, times(1)).delete(999L);
    }

    @Test
    @DisplayName("DELETE /employees/{id} - should return 404 when id is null")
    void deleteEmployee_WhenIdIsNull_ShouldReturn404() {
        // Act
        ResponseEntity<Map<String, Boolean>> response = employeeController.deleteEmployee(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(employeeService, never()).delete(any());
    }

    @Test
    @DisplayName("POST /give-dundie-awards/{organizationId} - should increment awards successfully")
    void giveDundieAwards_WhenOrganizationExists_ShouldReturnOk() {
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
    void giveDundieAwards_WhenNoEmployeesUpdated_ShouldReturn404() {
        // Arrange
        when(employeeService.incrementDundieAwardsForAll(1L)).thenReturn(0L);

        // Act
        ResponseEntity<OrganizationInfo> response = employeeController.giveDundieAwards(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(employeeService, times(1)).incrementDundieAwardsForAll(1L);
    }

    @Test
    @DisplayName("POST /give-dundie-awards/{organizationId} - should return 404 when organization id is null")
    void giveDundieAwards_WhenOrganizationIdIsNull_ShouldReturn404() {
        // Act
        ResponseEntity<OrganizationInfo> response = employeeController.giveDundieAwards(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(employeeService, never()).incrementDundieAwardsForAll(any());
    }
}
