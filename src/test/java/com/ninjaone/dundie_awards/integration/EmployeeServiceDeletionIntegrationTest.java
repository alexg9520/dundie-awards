package com.ninjaone.dundie_awards.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;
import com.ninjaone.dundie_awards.services.EmployeeService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Employee Service Deletion Integration Tests")
class EmployeeServiceDeletionIntegrationTest {

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

    private Organization testOrganization;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create baseline test data
        testOrganization = new Organization("Dunder Mifflin");
        testOrganization = organizationRepository.save(testOrganization);

        testEmployee = new Employee("Michael", "Scott", testOrganization);
        testEmployee.setDundieAwards(5);
        testEmployee = employeeRepository.save(testEmployee);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration - deleteEmployeesAndOrganization should delete organization with multiple employees")
    void deleteEmployeesAndOrganization_ShouldDeleteAllEmployeesAndOrganization() throws LookupException, InvalidArgumentException {
        // Arrange - create a new organization with multiple employees
        Organization branchOffice = new Organization("Scranton Branch Office");
        branchOffice = organizationRepository.save(branchOffice);
        
        Employee emp1 = new Employee("Dwight", "Schrute", branchOffice);
        emp1.setDundieAwards(7);
        emp1 = employeeRepository.save(emp1);

        Employee emp2 = new Employee("Jim", "Halpert", branchOffice);
        emp2.setDundieAwards(3);
        emp2 = employeeRepository.save(emp2);

        Employee emp3 = new Employee("Pam", "Beesly", branchOffice);
        emp3.setDundieAwards(2);
        emp3 = employeeRepository.save(emp3);

        Long branchId = branchOffice.getId();
        Long emp1Id = emp1.getId();
        Long emp2Id = emp2.getId();
        Long emp3Id = emp3.getId();
        
        // Verify setup - employees and organization exist
        assertEquals(4, employeeRepository.count()); // 3 new + 1 from setUp
        assertEquals(2, organizationRepository.count()); // 1 new + 1 from setUp

        // Act
        OrganizationInfo deletedOrg = employeeService.deleteEmployeesAndOrganization(branchId);

        // Assert - verify returned info is correct
        assertNotNull(deletedOrg);
        assertEquals(branchId, deletedOrg.id());
        assertEquals("Scranton Branch Office", deletedOrg.name());

        // Verify all employees from that organization are deleted
        assertTrue(employeeRepository.findById(emp1Id).isEmpty(), "Employee 1 should be deleted");
        assertTrue(employeeRepository.findById(emp2Id).isEmpty(), "Employee 2 should be deleted");
        assertTrue(employeeRepository.findById(emp3Id).isEmpty(), "Employee 3 should be deleted");
        
        // Verify the organization is deleted
        assertTrue(organizationRepository.findById(branchId).isEmpty(), "Organization should be deleted");

        // Verify only the specific organization's data was deleted (testOrganization should still exist)
        assertEquals(1, employeeRepository.count(), "Only employees from deleted org should be removed");
        assertEquals(1, organizationRepository.count(), "Only the target organization should be deleted");
        assertNotNull(employeeRepository.findById(testEmployee.getId()).orElse(null), "Test employee should still exist");
        assertNotNull(organizationRepository.findById(testOrganization.getId()).orElse(null), "Test organization should still exist");
    }

    @Test
    @DisplayName("Integration - deleteEmployeesAndOrganization with non-existent org should throw exception")
    void deleteEmployeesAndOrganization_WithNonExistentOrg_ShouldThrowException() {
        // Arrange
        Long nonExistentOrgId = 9999L;

        // Act & Assert
        assertThatThrownBy(() -> employeeService.deleteEmployeesAndOrganization(nonExistentOrgId))
                .isInstanceOf(LookupException.class);

        // Verify nothing was deleted
        assertNotNull(employeeRepository.findById(testEmployee.getId()).orElse(null));
        assertNotNull(organizationRepository.findById(testOrganization.getId()).orElse(null));
    }

    @Test
    @DisplayName("Integration - deleteEmployeesAndOrganization with no employees should still delete organization")
    void deleteEmployeesAndOrganization_WithNoEmployees_ShouldDeleteOrganization() throws LookupException, InvalidArgumentException {
        // Arrange - create empty organization
        Organization emptyOrg = new Organization("Empty Branch");
        emptyOrg = organizationRepository.save(emptyOrg);
        Long emptyOrgId = emptyOrg.getId();

        Long organizationCount = organizationRepository.count();

        // Act
        OrganizationInfo deletedOrg = employeeService.deleteEmployeesAndOrganization(emptyOrgId);

        // Assert
        assertNotNull(deletedOrg);
        assertEquals("Empty Branch", deletedOrg.name());
        assertTrue(organizationRepository.findById(emptyOrgId).isEmpty());
        assertEquals(organizationCount - 1, organizationRepository.count());
    }
}
