package com.ninjaone.dundie_awards.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.util.ReflectionTestUtils;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.ActivityInfo;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Service Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private EmployeeService employeeService;

    private Organization testOrganization;
    private Employee testEmployee;
    private OrganizationInfo testOrganizationInfo;
    private EmployeeInfo testEmployeeInfo;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization("Dunder Mifflin");
        testOrganization.setId(1L);

        testEmployee = new Employee("Michael", "Scott", testOrganization);
        testEmployee.setId(1L);
        testEmployee.setDundieAwards(5);

        testOrganizationInfo = OrganizationInfo.builder()
                .id(1L)
                .name("Dunder Mifflin")
                .build();

        testEmployeeInfo = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scott")
                .organization(testOrganizationInfo)
                .dundieAwards(5)
                .build();
    }

    @Test
    @DisplayName("findAll - should return all employees as EmployeeInfo")
    void findAll_ShouldReturnAllEmployees() {
        // Arrange
        Employee employee2 = new Employee("Jim", "Halpert", testOrganization);
        employee2.setId(2L);
        employee2.setDundieAwards(3);

        when(employeeRepository.findAll()).thenReturn(Arrays.asList(testEmployee, employee2));

        // Act
        List<EmployeeInfo> result = employeeService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertEquals("Michael", result.get(0).firstName());
        assertEquals("Jim", result.get(1).firstName());
        verify(employeeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getEmployeeInfoById - should return employee when found")
    void getEmployeeInfoById_WhenEmployeeExists_ShouldReturnEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        EmployeeInfo result = employeeService.getEmployeeInfoById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Michael", result.firstName());
        assertEquals("Scott", result.lastName());
        assertEquals(5, result.dundieAwards());
        verify(employeeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getEmployeeInfoById - should throw exception when id is null")
    void getEmployeeInfoById_WhenIdIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeInfoById(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No employee ID was provided");

        verify(employeeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getEmployeeInfoById - should throw exception when employee not found")
    void getEmployeeInfoById_WhenEmployeeNotFound_ShouldThrowException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeInfoById(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The employee was not found");

        verify(employeeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("save - should save new employee successfully")
    void save_WithValidEmployeeInfo_ShouldSaveEmployee() {
        // Arrange
        EmployeeInfo newEmployeeInfo = EmployeeInfo.builder()
                .firstName("Pam")
                .lastName("Beesly")
                .organization(testOrganizationInfo)
                .dundieAwards(0)
                .build();

        Employee savedEmployee = new Employee("Pam", "Beesly", testOrganization);
        savedEmployee.setId(3L);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // Act
        Employee result = employeeService.save(newEmployeeInfo);

        // Assert
        assertNotNull(result);
        assertEquals("Pam", result.getFirstName());
        assertEquals("Beesly", result.getLastName());
        verify(organizationRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("save - should throw exception when employee info is null")
    void save_WhenEmployeeInfoIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.save(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No employee information was provided");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("save - should throw exception when organization not found")
    void save_WhenOrganizationNotFound_ShouldThrowException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        EmployeeInfo newEmployeeInfo = EmployeeInfo.builder()
                .firstName("Pam")
                .lastName("Beesly")
                .organization(OrganizationInfo.builder().id(999L).name("Unknown").build())
                .dundieAwards(0)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> employeeService.save(newEmployeeInfo))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should update employee successfully")
    void update_WhenEmployeeExists_ShouldUpdateEmployee() {
        // Arrange
        EmployeeInfo updatedInfo = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scarn")
                .organization(testOrganizationInfo)
                .dundieAwards(5)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        // Act
        EmployeeInfo result = employeeService.update(1L, updatedInfo);

        // Assert
        assertNotNull(result);
        assertEquals("Scarn", result.lastName());
        verify(employeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("update - should throw exception when id is null")
    void update_WhenIdIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(null, testEmployeeInfo))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No employee ID was provided");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should throw exception when employee info is null")
    void update_WhenEmployeeInfoIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(1L, null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No employee information was provided");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should throw exception when employee not found")
    void update_WhenEmployeeNotFound_ShouldThrowException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(999L, testEmployeeInfo))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The employee was not found");

        verify(employeeRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete employee successfully")
    void delete_WhenEmployeeExists_ShouldDeleteEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        EmployeeInfo result = employeeService.delete(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Michael", result.firstName());
        verify(employeeRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).delete(testEmployee);
    }

    @Test
    @DisplayName("delete - should throw exception when id is null")
    void delete_WhenIdIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.delete(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No employee ID was provided");

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - should throw exception when employee not found")
    void delete_WhenEmployeeNotFound_ShouldThrowException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.delete(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The employee was not found");

        verify(employeeRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("incrementDundieAwardsForAll - should increment awards for organization")
    void incrementDundieAwardsForAll_ShouldIncrementAwards() {
        ReflectionTestUtils.setField(employeeService, "activityBindingName", "activity-out-0");

        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(employeeRepository.incrementDundieAwardsForAll(1L)).thenReturn(5L);
        when(streamBridge.send(anyString(), any(ActivityInfo.class))).thenReturn(true);

        // Act
        Long result = employeeService.incrementDundieAwardsForAll(1L);

        // Assert
        assertEquals(5L, result);
        verify(organizationRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).incrementDundieAwardsForAll(1L);
        verify(streamBridge, times(1)).send(anyString(), any(ActivityInfo.class));
    }

    @Test
    @DisplayName("incrementDundieAwardsForAll - should throw exception when organization not found")
    void incrementDundieAwardsForAll_WhenOrganizationNotFound_ShouldThrowException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.incrementDundieAwardsForAll(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).incrementDundieAwardsForAll(any());
    }

    @Test
    @DisplayName("getTotalAwardsByOrganization - should return total awards")
    void getTotalAwardsByOrganization_ShouldReturnTotalAwards() {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(employeeRepository.getTotalAwardsByOrganization(1L)).thenReturn(Optional.of(25L));

        // Act
        Long result = employeeService.getTotalAwardsByOrganization(1L);

        // Assert
        assertThat(result).isEqualTo(25L);
        verify(organizationRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(1L);
    }

    @Test
    @DisplayName("getTotalAwardsByOrganization - should return 0 when no awards found")
    void getTotalAwardsByOrganization_WhenNoAwards_ShouldReturnZero() {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(employeeRepository.getTotalAwardsByOrganization(1L)).thenReturn(Optional.empty());

        // Act
        Long result = employeeService.getTotalAwardsByOrganization(1L);

        // Assert
        assertThat(result).isEqualTo(0L);
        verify(organizationRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(1L);
    }

    @Test
    @DisplayName("getTotalAwards - should return total awards across all organizations")
    void getTotalAwards_ShouldReturnTotalAwards() {
        // Arrange
        when(employeeRepository.getTotalAwards()).thenReturn(Optional.of(100L));

        // Act
        Long result = employeeService.getTotalAwards();

        // Assert
        assertThat(result).isEqualTo(100L);
        verify(employeeRepository, times(1)).getTotalAwards();
    }

    @Test
    @DisplayName("getTotalAwards - should return 0 when no awards found")
    void getTotalAwards_WhenNoAwards_ShouldReturnZero() {
        // Arrange
        when(employeeRepository.getTotalAwards()).thenReturn(Optional.empty());

        // Act
        Long result = employeeService.getTotalAwards();

        // Assert
        assertThat(result).isEqualTo(0L);
        verify(employeeRepository, times(1)).getTotalAwards();
    }

    @Test
    @DisplayName("createEmployeeInfoFromEmployee - should convert employee to info")
    void createEmployeeInfoFromEmployee_ShouldConvertEmployee() {
        // Act
        EmployeeInfo result = employeeService.createEmployeeInfoFromEmployee(testEmployee);

        // Assert
        assertNotNull(result);
        assertEquals("Michael", result.firstName());
        assertEquals("Scott", result.lastName());
        assertEquals(5, result.dundieAwards());
        assertEquals("Dunder Mifflin", result.organization().name());
    }

    @Test
    @DisplayName("createEmployeeInfoFromEmployee - should throw exception when employee is null")
    void createEmployeeInfoFromEmployee_WhenEmployeeIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployeeInfoFromEmployee(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No employee was provided");
    }

    @Test
    @DisplayName("createOrganizationInfoFromOrganization - should convert organization to info")
    void createOrganizationInfoFromOrganization_ShouldConvertOrganization() {
        // Act
        OrganizationInfo result = employeeService.createOrganizationInfoFromOrganization(testOrganization);

        // Assert
        assertNotNull(result);
        assertEquals("Dunder Mifflin", result.name());
        assertEquals(1L, result.id());
    }

    @Test
    @DisplayName("createOrganizationInfoFromOrganization - should throw exception when organization is null")
    void createOrganizationInfoFromOrganization_WhenOrganizationIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> employeeService.createOrganizationInfoFromOrganization(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization was provided");
    }
}
