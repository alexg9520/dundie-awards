package com.ninjaone.dundie_awards.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import com.ninjaone.dundie_awards.services.EmployeeService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Employee Service Integration Tests")
class EmployeeServiceIntegrationTest {

	@Container
    public static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityManager entityManager;

    private Organization testOrganization;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        // Clean up database
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test data
        testOrganization = new Organization("Dunder Mifflin");
        testOrganization = organizationRepository.save(testOrganization);

        testEmployee = new Employee("Michael", "Scott", testOrganization);
        testEmployee.setDundieAwards(5);
        testEmployee = employeeRepository.save(testEmployee);
    }

    @Test
    @DisplayName("Integration - save should persist employee to database")
    void save_ShouldPersistToDatabase() throws LookupException, InvalidArgumentException {
        // Arrange
        OrganizationInfo orgInfo = OrganizationInfo.builder()
                .id(testOrganization.getId())
                .name(testOrganization.getName())
                .build();

        EmployeeInfo newEmployee = EmployeeInfo.builder()
                .firstName("Jim")
                .lastName("Halpert")
                .organization(orgInfo)
                .build();

        // Act
        EmployeeInfo saved = employeeService.save(newEmployee);

        // Assert
        assertNotNull(saved);
        assertNotNull(saved.id());
        assertEquals("Jim", saved.firstName());
        assertEquals("Halpert", saved.lastName());

        // Verify in database
        Employee dbEmployee = employeeRepository.findById(saved.id()).orElseThrow();
        assertEquals("Jim", dbEmployee.getFirstName());
        assertEquals("Halpert", dbEmployee.getLastName());
        assertEquals(testOrganization.getId(), dbEmployee.getOrganization().getId());
    }

    @Test
    @DisplayName("Integration - getEmployeeInfoById should retrieve from database")
    void getEmployeeInfoById_ShouldRetrieveFromDatabase() throws LookupException, InvalidArgumentException {
        // Act
        EmployeeInfo result = employeeService.getEmployeeInfoById(testEmployee.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testEmployee.getId(), result.id());
        assertEquals("Michael", result.firstName());
        assertEquals("Scott", result.lastName());
        assertEquals(5, result.dundieAwards());
        assertEquals("Dunder Mifflin", result.organization().name());
    }

    @Test
    @DisplayName("Integration - update should modify database record")
    void update_ShouldModifyDatabaseRecord() throws LookupException, InvalidArgumentException {
        // Arrange
        OrganizationInfo orgInfo = OrganizationInfo.builder()
                .id(testOrganization.getId())
                .name(testOrganization.getName())
                .build();

        EmployeeInfo updateInfo = EmployeeInfo.builder()
                .id(testEmployee.getId())
                .firstName("Michael")
                .lastName("Scarn")
                .organization(orgInfo)
                .dundieAwards(5)
                .build();

        // Act
        EmployeeInfo updated = employeeService.update(testEmployee.getId(), updateInfo);

        // Assert
        assertEquals("Scarn", updated.lastName());

        // Verify in database
        Employee dbEmployee = employeeRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals("Scarn", dbEmployee.getLastName());
    }

    @Test
    @DisplayName("Integration - delete should remove from database")
    void delete_ShouldRemoveFromDatabase() throws LookupException, InvalidArgumentException {
        // Arrange
        Long employeeId = testEmployee.getId();

        // Act
        EmployeeInfo deleted = employeeService.delete(employeeId);

        // Assert
        assertEquals("Michael", deleted.firstName());

        // Verify removed from database
        assertTrue(employeeRepository.findById(employeeId).isEmpty());
    }

    @Test
    @DisplayName("Integration - findAll should return paginated results from database")
    void findAll_ShouldReturnPaginatedResults() throws InvalidArgumentException {
        // Arrange - create additional employees
        Employee emp2 = new Employee("Jim", "Halpert", testOrganization);
        employeeRepository.save(emp2);
        
        Employee emp3 = new Employee("Pam", "Beesly", testOrganization);
        employeeRepository.save(emp3);

        // Act
        Page<EmployeeInfo> result = employeeService.findAll(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Integration - incrementDundieAwardsForAll should update all employees in organization")
    void incrementDundieAwardsForAll_ShouldUpdateAllEmployees() throws LookupException, InvalidArgumentException {
        // Arrange - create additional employees
        Employee emp2 = new Employee("Jim", "Halpert", testOrganization);
        emp2.setDundieAwards(3);
        employeeRepository.save(emp2);

        Employee emp3 = new Employee("Pam", "Beesly", testOrganization);
        emp3.setDundieAwards(2);
        employeeRepository.save(emp3);

        // Act
        Long updated = employeeService.incrementDundieAwardsForAll(testOrganization.getId());

        // Assert
        assertEquals(3L, updated);

        // Clear the persistence context to force fresh reads from database
        entityManager.flush();
        entityManager.clear();

        // Verify in database
        Employee updatedEmp1 = employeeRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals(6, updatedEmp1.getDundieAwards());

        Employee updatedEmp2 = employeeRepository.findById(emp2.getId()).orElseThrow();
        assertEquals(4, updatedEmp2.getDundieAwards());

        Employee updatedEmp3 = employeeRepository.findById(emp3.getId()).orElseThrow();
        assertEquals(3, updatedEmp3.getDundieAwards());
    }

    @Test
    @DisplayName("Integration - getTotalAwardsByOrganization should calculate from database")
    void getTotalAwardsByOrganization_ShouldCalculateFromDatabase() throws LookupException, InvalidArgumentException {
        // Arrange
        Employee emp2 = new Employee("Jim", "Halpert", testOrganization);
        emp2.setDundieAwards(3);
        employeeRepository.save(emp2);

        Employee emp3 = new Employee("Pam", "Beesly", testOrganization);
        emp3.setDundieAwards(2);
        employeeRepository.save(emp3);

        // Act
        Long total = employeeService.getTotalAwardsByOrganization(testOrganization.getId());

        // Assert
        assertEquals(10L, total); // 5 + 3 + 2
    }

    @Test
    @DisplayName("Integration - getTotalAwards should calculate across all organizations")
    void getTotalAwards_ShouldCalculateAcrossAllOrganizations() throws LookupException, InvalidArgumentException {
        // Arrange - create another organization with employees
        Organization org2 = new Organization("Stamford Branch");
        org2 = organizationRepository.save(org2);

        Employee emp2 = new Employee("Karen", "Filippelli", org2);
        emp2.setDundieAwards(4);
        employeeRepository.save(emp2);

        // Act
        Long total = employeeService.getTotalAwards();

        // Assert
        assertEquals(9L, total); // 5 + 4
    }

    @Test
    @DisplayName("Integration - update organization should maintain referential integrity")
    void update_WithOrganizationChange_ShouldMaintainReferentialIntegrity() throws LookupException, InvalidArgumentException {
        // Arrange - create new organization
        Organization newOrg = new Organization("Stamford Branch");
        newOrg = organizationRepository.save(newOrg);

        OrganizationInfo newOrgInfo = OrganizationInfo.builder()
                .id(newOrg.getId())
                .name(newOrg.getName())
                .build();

        EmployeeInfo updateInfo = EmployeeInfo.builder()
                .id(testEmployee.getId())
                .firstName("Michael")
                .lastName("Scott")
                .organization(newOrgInfo)
                .dundieAwards(5)
                .build();

        // Act
        EmployeeInfo updated = employeeService.update(testEmployee.getId(), updateInfo);

        // Assert
        assertEquals(newOrg.getId(), updated.organization().id());

        // Verify in database
        Employee dbEmployee = employeeRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals(newOrg.getId(), dbEmployee.getOrganization().getId());
    }

    @Test
    @DisplayName("Integration - cascade operations should work correctly")
    void cascadeOperations_ShouldWorkCorrectly() throws LookupException, InvalidArgumentException {
        // Arrange
        Long employeeCount = employeeRepository.count();

        // Act - save creates cascade
        OrganizationInfo orgInfo = OrganizationInfo.builder()
                .id(testOrganization.getId())
                .name(testOrganization.getName())
                .build();

        EmployeeInfo newEmployee = EmployeeInfo.builder()
                .firstName("Dwight")
                .lastName("Schrute")
                .organization(orgInfo)
                .build();

        employeeService.save(newEmployee);

        // Assert
        assertEquals(employeeCount + 1, employeeRepository.count());
    }

    @Test
    @DisplayName("Integration - transactional rollback on exception")
    void transactionalRollback_OnException() throws LookupException {
        // Arrange
        Long initialCount = employeeRepository.count();

        OrganizationInfo invalidOrg = OrganizationInfo.builder()
                .id(999L) // Non-existent organization
                .name("Invalid Org")
                .build();

        EmployeeInfo newEmployee = EmployeeInfo.builder()
                .firstName("Invalid")
                .lastName("Employee")
                .organization(invalidOrg)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> employeeService.save(newEmployee))
                .isInstanceOf(LookupException.class);

        // Verify rollback - count should be unchanged
        assertEquals(initialCount, employeeRepository.count());
    }

    @Test
    @DisplayName("Integration - getEmployeeCount should return correct count")
    void getEmployeeCount_ShouldReturnCorrectCount() {
        // Arrange
        Employee emp2 = new Employee("Jim", "Halpert", testOrganization);
        employeeRepository.save(emp2);

        // Act
        long count = employeeService.getEmployeeCount();
        long orgCount = employeeService.getEmployeeCount(testOrganization.getId());

        // Assert
        assertEquals(2, count);
        assertEquals(2, orgCount);
    }

    @Test
    @DisplayName("Integration - delete should handle constraints properly")
    void delete_ShouldHandleConstraints() throws LookupException, InvalidArgumentException {
        // This test verifies that delete works when there are no constraints
        // In a real scenario, you might have constraints that prevent deletion
        
        // Arrange
        Long employeeId = testEmployee.getId();
        Long initialCount = employeeRepository.count();

        // Act
        employeeService.delete(employeeId);

        // Assert
        assertEquals(initialCount - 1, employeeRepository.count());
        assertTrue(employeeRepository.findById(employeeId).isEmpty());
    }
}
