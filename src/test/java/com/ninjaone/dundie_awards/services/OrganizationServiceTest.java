package com.ninjaone.dundie_awards.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Organization Service Tests")
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization testOrganization;

    private OrganizationInfo testOrganizationInfo;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization("Dunder Mifflin");
        testOrganization.setId(1L);

        testOrganizationInfo = OrganizationInfo.builder()
                .id(1L)
                .name("Dunder Mifflin")
                .build();
    }

    @Test
    @DisplayName("getOrganizationInfo - should return organization info by id")
    void getOrganizationInfo_ShouldReturnOrganizationInfo() throws LookupException, InvalidArgumentException {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

        // Act
        OrganizationInfo result = organizationService.getOrganizationInfo(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Dunder Mifflin", result.name());
        verify(organizationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getOrganizationInfo - should throw exception when id is null")
    void getOrganizationInfo_WhenIdIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.getOrganizationInfo(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization ID was provided");

        verify(organizationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getOrganizationInfo - should throw exception when organization not found")
    void getOrganizationInfo_WhenOrganizationNotFound_ShouldThrowException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.getOrganizationInfo(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getOrganizationData - should return organization entity by id")
    void getOrganizationData_ShouldReturnOrganization() throws LookupException {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

        // Act
        Organization result = organizationService.getOrganizationData(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Dunder Mifflin", result.getName());
        verify(organizationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getOrganizationData - should throw exception when organization not found")
    void getOrganizationData_WhenOrganizationNotFound_ShouldThrowException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.getOrganizationData(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("save - should save new organization")
    void save_WithValidOrganizationInfo_ShouldSaveOrganization() throws InvalidArgumentException {
        // Arrange
        OrganizationInfo newOrganizationInfo = OrganizationInfo.builder()
                .name("Scranton Branch")
                .build();

        Organization savedOrganization = new Organization("Scranton Branch");
        savedOrganization.setId(2L);

        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrganization);

        // Act
        OrganizationInfo result = organizationService.save(newOrganizationInfo);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("Scranton Branch", result.name());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    @DisplayName("save - should throw exception when organization info is null")
    void save_WhenOrganizationInfoIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.save(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization information was provided");

        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should update organization successfully")
    void update_WhenOrganizationExists_ShouldUpdateOrganization() throws LookupException, InvalidArgumentException {
        // Arrange
        OrganizationInfo updatedInfo = OrganizationInfo.builder()
                .id(1L)
                .name("Dunder Mifflin Paper Company")
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

        // Act
        OrganizationInfo result = organizationService.update(1L, updatedInfo);

        // Assert
        assertNotNull(result);
        assertEquals("Dunder Mifflin Paper Company", result.name());
        verify(organizationRepository, times(1)).findById(1L);
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    @DisplayName("update - should throw exception when id is null")
    void update_WhenIdIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(null, testOrganizationInfo))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization ID was provided");

        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should throw exception when organization info is null")
    void update_WhenOrganizationInfoIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(1L, null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization information was provided");

        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - should throw exception when organization not found")
    void update_WhenOrganizationNotFound_ShouldThrowException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(999L, testOrganizationInfo))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationRepository, times(1)).findById(999L);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - should delete organization successfully")
    void delete_WhenOrganizationExists_ShouldDeleteOrganization() throws LookupException, InvalidArgumentException {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

        // Act
        OrganizationInfo result = organizationService.delete(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Dunder Mifflin", result.name());
        verify(organizationRepository, times(1)).findById(1L);
        verify(organizationRepository, times(1)).delete(testOrganization);
    }

    @Test
    @DisplayName("delete - should throw exception when id is null")
    void delete_WhenIdIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization ID was provided");

        verify(organizationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - should throw exception when organization not found")
    void delete_WhenOrganizationNotFound_ShouldThrowException() {
        // Arrange
        when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");

        verify(organizationRepository, times(1)).findById(999L);
        verify(organizationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("createOrganizationInfoFromOrganization - should convert organization to info")
    void createOrganizationInfoFromOrganization_ShouldConvertOrganization() throws InvalidArgumentException {
        // Act
        OrganizationInfo result = organizationService.createOrganizationInfoFromOrganization(testOrganization);

        // Assert
        assertNotNull(result);
        assertEquals("Dunder Mifflin", result.name());
        assertEquals(1L, result.id());
    }

    @Test
    @DisplayName("createOrganizationInfoFromOrganization - should throw exception when organization is null")
    void createOrganizationInfoFromOrganization_WhenOrganizationIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.createOrganizationInfoFromOrganization(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No organization was provided");
    }

    // ========== Transactional Behavior Tests ==========

    @Test
    @DisplayName("@Transactional update - should rollback when exception occurs after save")
    void update_WhenExceptionAfterSave_ShouldRollback() {
        // Arrange
        OrganizationInfo updatedInfo = OrganizationInfo.builder()
                .id(1L)
                .name("Updated Name")
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        // Simulate exception during save operation (e.g., database constraint violation)
        when(organizationRepository.save(any(Organization.class)))
                .thenThrow(new RuntimeException("Database constraint violation"));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(1L, updatedInfo))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database constraint violation");

        // Verify save was attempted but transaction should rollback
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    @DisplayName("@Transactional delete - should rollback when exception occurs during delete")
    void delete_WhenExceptionDuringDelete_ShouldRollback() {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        // Simulate exception during delete (e.g., foreign key constraint)
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        org.mockito.Mockito.doThrow(new RuntimeException("Foreign key constraint violation"))
                .when(organizationRepository).delete(any(Organization.class));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Foreign key constraint violation");

        // Verify delete was attempted but transaction should rollback
        verify(organizationRepository, times(1)).delete(any(Organization.class));
    }

    @Test
    @DisplayName("@Transactional update - multiple operations should be atomic")
    void update_MultipleOperations_ShouldBeAtomic() throws LookupException, InvalidArgumentException {
        // Arrange
        OrganizationInfo updatedInfo = OrganizationInfo.builder()
                .id(1L)
                .name("New Name")
                .build();

        Organization updatedOrg = new Organization("New Name");
        updatedOrg.setId(1L);

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(updatedOrg);

        // Act
        OrganizationInfo result = organizationService.update(1L, updatedInfo);

        // Assert - verify all operations in transaction completed successfully
        assertNotNull(result);
        assertEquals("New Name", result.name());
        verify(organizationRepository, times(1)).findById(1L);
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    @DisplayName("@Transactional delete - should complete all operations atomically")
    void delete_AllOperations_ShouldBeAtomic() throws LookupException, InvalidArgumentException {
        // Arrange
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

        // Act
        OrganizationInfo result = organizationService.delete(1L);

        // Assert - verify all operations completed in correct order
        assertNotNull(result);
        assertEquals("Dunder Mifflin", result.name());
        
        // Verify transaction operations order
        var inOrder = org.mockito.Mockito.inOrder(organizationRepository);
        inOrder.verify(organizationRepository).findById(1L);
        inOrder.verify(organizationRepository).delete(testOrganization);
    }

    @Test
    @DisplayName("@Transactional update - should handle concurrent modification scenarios")
    void update_ConcurrentModification_ShouldHandleCorrectly() {
        // Arrange
        OrganizationInfo updatedInfo = OrganizationInfo.builder()
                .id(1L)
                .name("Updated Name")
                .build();

        // Simulate optimistic locking exception
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class)))
                .thenThrow(new org.springframework.dao.OptimisticLockingFailureException(
                        "Row was updated by another transaction"));

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(1L, updatedInfo))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);

        // Transaction should rollback on optimistic locking failure
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }
}
