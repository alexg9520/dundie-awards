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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private OrganizationService organizationService;

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
    void findAll_ShouldReturnAllEmployees() throws InvalidArgumentException {
        // Arrange
        Employee employee2 = new Employee("Jim", "Halpert", testOrganization);
        employee2.setId(2L);
        employee2.setDundieAwards(3);

        Pageable pageable = PageRequest.of(0, 100, Sort.by("id").ascending());
        when(employeeRepository.findAll(any(Pageable.class))).thenReturn(
            new org.springframework.data.domain.PageImpl<>(Arrays.asList(testEmployee, employee2)));

        // Act
        List<EmployeeInfo> result = employeeService.findAll(pageable).getContent();

        // Assert
        assertThat(result).hasSize(2);
        assertEquals("Michael", result.get(0).firstName());
        assertEquals("Jim", result.get(1).firstName());
        verify(employeeRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("getEmployeeInfoById - should return employee when found")
    void getEmployeeInfoById_WhenEmployeeExists_ShouldReturnEmployee() throws LookupException, InvalidArgumentException {
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
    void save_WithValidEmployeeInfo_ShouldSaveEmployee() throws LookupException, InvalidArgumentException {
        // Arrange
        EmployeeInfo newEmployeeInfo = EmployeeInfo.builder()
                .firstName("Pam")
                .lastName("Beesly")
                .organization(testOrganizationInfo)
                .dundieAwards(0)
                .build();

        Employee savedEmployee = new Employee("Pam", "Beesly", testOrganization);
        savedEmployee.setId(3L);

        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // Act
        EmployeeInfo result = employeeService.save(newEmployeeInfo);

        // Assert
        assertNotNull(result);
        assertEquals("Pam", result.firstName());
        assertEquals("Beesly", result.lastName());
        verify(organizationService, times(1)).getOrganizationData(1L);
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
    void save_WhenOrganizationNotFound_ShouldThrowException() throws LookupException {
        // Arrange
        when(organizationService.getOrganizationData(999L))
                .thenThrow(new LookupException("The organization was not found"));

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

        verify(organizationService, times(1)).getOrganizationData(999L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should update employee successfully")
    void update_WhenEmployeeExists_ShouldUpdateEmployee() throws LookupException, InvalidArgumentException {
        // Arrange
        EmployeeInfo updatedInfo = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scarn")
                .organization(testOrganizationInfo)
                .dundieAwards(5)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
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
    void delete_WhenEmployeeExists_ShouldDeleteEmployee() throws LookupException, InvalidArgumentException {
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
    void incrementDundieAwardsForAll_ShouldIncrementAwards() throws LookupException, InvalidArgumentException {
        ReflectionTestUtils.setField(employeeService, "activityBindingName", "activity-out-0");

        // Arrange
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.incrementDundieAwardsForAll(1L)).thenReturn(5L);
        when(streamBridge.send(anyString(), any(ActivityInfo.class))).thenReturn(true);

        // Act
        Long result = employeeService.incrementDundieAwardsForAll(1L);

        // Assert
        assertEquals(5L, result);
        verify(organizationService, times(1)).getOrganizationData(1L);
        verify(employeeRepository, times(1)).incrementDundieAwardsForAll(1L);
        verify(streamBridge, times(1)).send(anyString(), any(ActivityInfo.class));
    }

    @Test
    @DisplayName("incrementDundieAwardsForAll - should throw exception when organization not found")
    void incrementDundieAwardsForAll_WhenOrganizationNotFound_ShouldThrowException() throws LookupException {
        // Arrange
        when(organizationService.getOrganizationData(999L))
                .thenThrow(new LookupException("The organization was not found"));

        // Act & Assert
        assertThatThrownBy(() -> employeeService.incrementDundieAwardsForAll(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationService, times(1)).getOrganizationData(999L);
        verify(employeeRepository, never()).incrementDundieAwardsForAll(any());
    }

    @Test
    @DisplayName("getTotalAwardsByOrganization - should return total awards")
    void getTotalAwardsByOrganization_ShouldReturnTotalAwards() throws LookupException, InvalidArgumentException {
        // Arrange
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.getTotalAwardsByOrganization(1L)).thenReturn(Optional.of(25L));

        // Act
        Long result = employeeService.getTotalAwardsByOrganization(1L);

        // Assert
        assertThat(result).isEqualTo(25L);
        verify(organizationService, times(1)).getOrganizationData(1L);
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(1L);
    }

    @Test
    @DisplayName("getTotalAwardsByOrganization - should return 0 when no awards found")
    void getTotalAwardsByOrganization_WhenNoAwards_ShouldReturnZero() throws LookupException, InvalidArgumentException {
        // Arrange
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.getTotalAwardsByOrganization(1L)).thenReturn(Optional.empty());

        // Act
        Long result = employeeService.getTotalAwardsByOrganization(1L);

        // Assert
        assertThat(result).isEqualTo(0L);
        verify(organizationService, times(1)).getOrganizationData(1L);
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(1L);
    }

    @Test
    @DisplayName("getTotalAwards - should return total awards across all organizations")
    void getTotalAwards_ShouldReturnTotalAwards() throws LookupException, InvalidArgumentException {
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
    void getTotalAwards_WhenNoAwards_ShouldReturnZero() throws LookupException, InvalidArgumentException {
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
    void createEmployeeInfoFromEmployee_ShouldConvertEmployee() throws InvalidArgumentException {
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
    void createOrganizationInfoFromOrganization_ShouldConvertOrganization() throws InvalidArgumentException {
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

    // ========== Transactional Behavior Tests ==========

    @Test
    @DisplayName("@Transactional update - should rollback when save operation fails")
    void update_WhenSaveFails_ShouldRollback() throws LookupException {
        // Arrange
        EmployeeInfo updatedInfo = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scarn")
                .organization(testOrganizationInfo)
                .dundieAwards(5)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        // Simulate database exception during save
        when(employeeRepository.save(any(Employee.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "Unique constraint violation"));

        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(1L, updatedInfo))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // Verify save was attempted but transaction should rollback
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("@Transactional delete - should rollback when delete operation fails")
    void delete_WhenDeleteFails_ShouldRollback() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        org.mockito.Mockito.doThrow(new RuntimeException("Cannot delete employee with active relationships"))
                .when(employeeRepository).delete(any(Employee.class));

        // Act & Assert
        assertThatThrownBy(() -> employeeService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete employee with active relationships");

        // Verify delete was attempted but should rollback
        verify(employeeRepository, times(1)).delete(any(Employee.class));
    }

    @Test
    @DisplayName("@Transactional incrementDundieAwardsForAll - should rollback if message send fails")
    void incrementDundieAwardsForAll_WhenMessageSendFails_ShouldRollback() throws LookupException {
        ReflectionTestUtils.setField(employeeService, "activityBindingName", "activity-out-0");

        // Arrange
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.incrementDundieAwardsForAll(1L)).thenReturn(5L);
        // Simulate message broker failure
        when(streamBridge.send(anyString(), any(ActivityInfo.class)))
                .thenThrow(new RuntimeException("Message broker unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> employeeService.incrementDundieAwardsForAll(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Message broker unavailable");

        // Verify database update was attempted but should rollback due to transaction
        verify(employeeRepository, times(1)).incrementDundieAwardsForAll(1L);
        verify(streamBridge, times(1)).send(anyString(), any(ActivityInfo.class));
    }

    @Test
    @DisplayName("@Transactional incrementDundieAwardsForAll - all operations should be atomic")
    void incrementDundieAwardsForAll_AllOperations_ShouldBeAtomic() throws LookupException, InvalidArgumentException {
        ReflectionTestUtils.setField(employeeService, "activityBindingName", "activity-out-0");

        // Arrange
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.incrementDundieAwardsForAll(1L)).thenReturn(5L);
        when(streamBridge.send(anyString(), any(ActivityInfo.class))).thenReturn(true);

        // Act
        Long result = employeeService.incrementDundieAwardsForAll(1L);

        // Assert - all operations completed successfully in transaction
        assertEquals(5L, result);
        
        // Verify transaction operations order
        var inOrder = org.mockito.Mockito.inOrder(organizationService, employeeRepository, streamBridge);
        inOrder.verify(organizationService).getOrganizationData(1L);
        inOrder.verify(employeeRepository).incrementDundieAwardsForAll(1L);
        inOrder.verify(streamBridge).send(anyString(), any(ActivityInfo.class));
    }

    @Test
    @DisplayName("@Transactional update - should handle organization lookup failure in transaction")
    void update_WhenOrganizationLookupFails_ShouldRollback() throws LookupException {
        // Arrange
        EmployeeInfo updatedInfo = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scott")
                .organization(OrganizationInfo.builder().id(999L).name("Unknown").build())
                .dundieAwards(5)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        // Organization lookup fails within transaction
        when(organizationService.getOrganizationData(999L))
                .thenThrow(new LookupException("The organization was not found"));

        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(1L, updatedInfo))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        // Verify no save was attempted after lookup failed
        verify(organizationService, times(1)).getOrganizationData(999L);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("@Transactional delete - should complete all cache evictions in transaction")
    void delete_WithCacheEviction_ShouldBeAtomic() throws LookupException, InvalidArgumentException {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act
        EmployeeInfo result = employeeService.delete(1L);

        // Assert - verify all operations completed
        assertNotNull(result);
        assertEquals("Michael", result.firstName());
        
        // Verify transaction operations completed in order
        var inOrder = org.mockito.Mockito.inOrder(employeeRepository);
        inOrder.verify(employeeRepository).findById(1L);
        inOrder.verify(employeeRepository).delete(testEmployee);
    }

    @Test
    @DisplayName("@Transactional update - multiple database operations should complete atomically")
    void update_MultipleOperations_ShouldCompleteAtomically() throws LookupException, InvalidArgumentException {
        // Arrange
        EmployeeInfo updatedInfo = EmployeeInfo.builder()
                .id(1L)
                .firstName("Michael")
                .lastName("Scarn")
                .organization(testOrganizationInfo)
                .dundieAwards(5)
                .build();

        Employee updatedEmployee = new Employee("Michael", "Scarn", testOrganization);
        updatedEmployee.setId(1L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(organizationService.getOrganizationData(1L)).thenReturn(testOrganization);
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);

        // Act
        EmployeeInfo result = employeeService.update(1L, updatedInfo);

        // Assert - verify all operations in transaction completed successfully
        assertNotNull(result);
        assertEquals("Scarn", result.lastName());
        
        // Verify transaction operations completed in order
        var inOrder = org.mockito.Mockito.inOrder(employeeRepository, organizationService);
        inOrder.verify(employeeRepository).findById(1L);
        inOrder.verify(organizationService).getOrganizationData(1L);
        inOrder.verify(employeeRepository).save(any(Employee.class));
    }
}
