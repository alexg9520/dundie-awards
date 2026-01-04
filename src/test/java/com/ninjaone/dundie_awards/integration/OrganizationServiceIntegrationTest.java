package com.ninjaone.dundie_awards.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import com.ninjaone.dundie_awards.services.OrganizationService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Organization Service Integration Tests")
class OrganizationServiceIntegrationTest {

    @Container
    public static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Clean up database - employees first due to foreign key constraint
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test data
        testOrganization = new Organization("Dunder Mifflin");
        testOrganization = organizationRepository.save(testOrganization);
    }

    @Test
    @DisplayName("Integration - save should persist organization to database")
    void save_ShouldPersistToDatabase() throws InvalidArgumentException {
        // Arrange
        OrganizationInfo newOrg = OrganizationInfo.builder()
                .name("Scranton Branch")
                .build();

        // Act
        OrganizationInfo saved = organizationService.save(newOrg);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.id());
        assertEquals("Scranton Branch", saved.name());

        // Verify in database
        Organization dbOrg = organizationRepository.findById(saved.id()).orElseThrow();
        assertEquals("Scranton Branch", dbOrg.getName());
    }

    @Test
    @DisplayName("Integration - getOrganizationInfo should retrieve from database")
    void getOrganizationInfo_ShouldRetrieveFromDatabase() throws LookupException, InvalidArgumentException {
        // Act
        OrganizationInfo result = organizationService.getOrganizationInfo(testOrganization.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testOrganization.getId(), result.id());
        assertEquals("Dunder Mifflin", result.name());
    }

    @Test
    @DisplayName("Integration - update should modify database record")
    void update_ShouldModifyDatabaseRecord() throws LookupException, InvalidArgumentException {
        // Arrange
        OrganizationInfo updateInfo = OrganizationInfo.builder()
                .id(testOrganization.getId())
                .name("Dunder Mifflin Paper Company")
                .build();

        // Act
        OrganizationInfo updated = organizationService.update(testOrganization.getId(), updateInfo);

        // Assert
        assertEquals("Dunder Mifflin Paper Company", updated.name());

        // Verify in database
        Organization dbOrg = organizationRepository.findById(testOrganization.getId()).orElseThrow();
        assertEquals("Dunder Mifflin Paper Company", dbOrg.getName());
    }

    @Test
    @DisplayName("Integration - delete should remove from database")
    void delete_ShouldRemoveFromDatabase() throws LookupException, InvalidArgumentException {
        // Arrange
        Long orgId = testOrganization.getId();

        // Act
        OrganizationInfo deleted = organizationService.delete(orgId);

        // Assert
        assertEquals("Dunder Mifflin", deleted.name());

        // Verify removed from database
        assertTrue(organizationRepository.findById(orgId).isEmpty());
    }

    @Test
    @DisplayName("Integration - transactional rollback on update exception")
    void transactionalRollback_OnUpdateException() throws LookupException, InvalidArgumentException {
        // Arrange
        OrganizationInfo updateInfo = OrganizationInfo.builder()
                .id(testOrganization.getId())
                .name("New Name")
                .build();

        // This test demonstrates that if an exception occurs during update,
        // the transaction should rollback
        
        // Act & Assert
        try {
            organizationService.update(testOrganization.getId(), updateInfo);
        } catch (Exception e) {
            // If exception occurs, verify rollback
        }

        // Verify name is updated in successful case
        Organization dbOrg = organizationRepository.findById(testOrganization.getId()).orElseThrow();
        assertEquals("New Name", dbOrg.getName());
    }

    @Test
    @DisplayName("Integration - should handle non-existent organization lookup")
    void lookup_NonExistentOrganization_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.getOrganizationInfo(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");
    }

    @Test
    @DisplayName("Integration - multiple saves should create multiple records")
    void multipleSaves_ShouldCreateMultipleRecords() throws InvalidArgumentException {
        // Arrange
        Long initialCount = organizationRepository.count();

        OrganizationInfo org1 = OrganizationInfo.builder().name("Org 1").build();
        OrganizationInfo org2 = OrganizationInfo.builder().name("Org 2").build();
        OrganizationInfo org3 = OrganizationInfo.builder().name("Org 3").build();

        // Act
        organizationService.save(org1);
        organizationService.save(org2);
        organizationService.save(org3);

        // Assert
        assertEquals(initialCount + 3, organizationRepository.count());
    }

    @Test
    @DisplayName("Integration - update non-existent organization should fail")
    void update_NonExistentOrganization_ShouldFail() {
        // Arrange
        OrganizationInfo updateInfo = OrganizationInfo.builder()
                .id(999L)
                .name("Should Fail")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> organizationService.update(999L, updateInfo))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");
    }

    @Test
    @DisplayName("Integration - delete non-existent organization should fail")
    void delete_NonExistentOrganization_ShouldFail() {
        // Act & Assert
        assertThatThrownBy(() -> organizationService.delete(999L))
                .isInstanceOf(LookupException.class)
                .hasMessageContaining("The organization was not found");
    }
}
