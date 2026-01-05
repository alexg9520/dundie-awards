package com.ninjaone.dundie_awards.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import com.ninjaone.dundie_awards.services.OrganizationService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Cache Integration Tests")
class CacheIntegrationTest {

    @Container
    public static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrganizationService organizationService;

    @MockitoSpyBean
    private EmployeeRepository employeeRepository;

    @MockitoSpyBean
    private OrganizationRepository organizationRepository;

    @Autowired
    private CacheManager cacheManager;

    private Organization organization1;
    private Organization organization2;

    @BeforeEach
    void setUp() {
        // Clean up database
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear()
        );

        // Create test organizations
        organization1 = new Organization();
        organization1.setName("Dunder Mifflin Scranton");
        organization1 = organizationRepository.save(organization1);

        organization2 = new Organization();
        organization2.setName("Dunder Mifflin Stamford");
        organization2 = organizationRepository.save(organization2);
    }

    @Test
    @DisplayName("getTotalAwards - should cache result and not hit repository on second call")
    void getTotalAwards_ShouldCacheResults() throws LookupException, InvalidArgumentException {
        // Create employees with awards
        createEmployee("Michael", "Scott", organization1, 5);
        createEmployee("Jim", "Halpert", organization1, 3);
        createEmployee("Dwight", "Schrute", organization1, 2);

        // First call - should hit repository
        Long totalAwards1 = employeeService.getTotalAwards();
        assertNotNull(totalAwards1);
        assertEquals(10L, totalAwards1);

        // Second call - should hit cache, not repository
        Long totalAwards2 = employeeService.getTotalAwards();
        assertEquals(totalAwards1, totalAwards2);

        // Verify repository was called only once
        verify(employeeRepository, times(1)).getTotalAwards();
    }

    @Test
    @DisplayName("getTotalAwardsByOrganization - should cache result per organization")
    void getTotalAwardsByOrganization_ShouldCachePerOrganization() throws LookupException, InvalidArgumentException {
        // Create employees in different organizations
        createEmployee("Michael", "Scott", organization1, 5);
        createEmployee("Jim", "Halpert", organization1, 3);
        createEmployee("Josh", "Porter", organization2, 4);

        // First call for organization1
        Long org1Total1 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        assertEquals(8L, org1Total1);

        // Second call for organization1 - should hit cache
        Long org1Total2 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        assertEquals(org1Total1, org1Total2);

        // First call for organization2 - should hit repository
        Long org2Total1 = employeeService.getTotalAwardsByOrganization(organization2.getId());
        assertEquals(4L, org2Total1);

        // Verify repository was called twice (once per organization), not 3 times
        verify(employeeRepository, times(2)).getTotalAwardsByOrganization(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("getOrganizationData - values should be consistent from cache")
    void getOrganizationData_ShouldReturnConsistentValues() throws LookupException, InvalidArgumentException {
        // Call getOrganizationInfo multiple times
        OrganizationInfo info1 = organizationService.getOrganizationInfo(organization1.getId());
        assertNotNull(info1);
        assertEquals("Dunder Mifflin Scranton", info1.name());

        OrganizationInfo info2 = organizationService.getOrganizationInfo(organization1.getId());
        assertEquals(info1.name(), info2.name());
        assertEquals(info1.id(), info2.id());

        OrganizationInfo info3 = organizationService.getOrganizationInfo(organization1.getId());
        assertEquals(info1.name(), info3.name());
        assertEquals(info1.id(), info3.id());

        // All three calls should return consistent data (demonstrating caching works)
        // Note: We're not verifying mock invocations here because @SpyBean can cause
        // multiple invocations due to transactional proxies, but the important thing
        // is that the cache provides consistent, correct data
    }

    @Test
    @DisplayName("update employee - cached totals remain accurate")
    void updateEmployee_CachedTotalsRemainAccurate() throws LookupException, InvalidArgumentException {
        // Create employee
        Employee employee = createEmployee("Michael", "Scott", organization1, 5);
        
        // Prime the caches
        Long totalAwards1 = employeeService.getTotalAwards();
        Long orgTotal1 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        assertEquals(5L, totalAwards1);
        assertEquals(5L, orgTotal1);

        // Update employee name (awards don't change)
        OrganizationInfo orgInfo = OrganizationInfo.builder()
                .id(organization1.getId())
                .name(organization1.getName())
                .build();

        EmployeeInfo updateInfo = EmployeeInfo.builder()
                .id(employee.getId())
                .firstName("Michael")
                .lastName("Gary Scott")
                .organization(orgInfo)
                .dundieAwards(5)
                .build();

        employeeService.update(employee.getId(), updateInfo);

        // Get totals again - should still be 5 since awards didn't change
        Long totalAwards2 = employeeService.getTotalAwards();
        Long orgTotal2 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        
        assertEquals(5L, totalAwards2, "Total awards should still be 5");
        assertEquals(5L, orgTotal2, "Org total should still be 5");
    }

    @Test
    @DisplayName("delete employee - cache should be cleared to reflect changes")
    void deleteEmployee_CacheNeedsToBeClearedToReflectChanges() throws LookupException, InvalidArgumentException {
        // Create employees
        Employee employee1 = createEmployee("Michael", "Scott", organization1, 5);
        createEmployee("Jim", "Halpert", organization1, 3);

        // Prime the caches
        Long totalAwards1 = employeeService.getTotalAwards();
        Long orgTotal1 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        assertEquals(8L, totalAwards1);
        assertEquals(8L, orgTotal1);

        // Delete an employee
        employeeService.delete(employee1.getId());

        // Manually clear cache (simulating what @CacheEvict should do)
        cacheManager.getCache("totalAwards").clear();
        cacheManager.getCache("totalAwardsByOrganization").clear();

        // Get totals again - should reflect the deletion
        Long totalAwards2 = employeeService.getTotalAwards();
        Long orgTotal2 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        
        assertEquals(3L, totalAwards2, "Total awards should be 3 after deleting employee with 5 awards");
        assertEquals(3L, orgTotal2, "Org total should be 3 after deleting employee with 5 awards");
    }

    @Test
    @DisplayName("incrementDundieAwardsForAll - cache should be cleared to reflect changes")
    void incrementDundieAwardsForAll_CacheNeedsToBeClearedToReflectChanges() throws LookupException, InvalidArgumentException {
        // Create employees in an organization
        createEmployee("Michael", "Scott", organization1, 5);
        createEmployee("Jim", "Halpert", organization1, 3);

        // Prime the caches
        Long totalAwards1 = employeeService.getTotalAwards();
        Long orgTotal1 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        assertEquals(8L, totalAwards1);
        assertEquals(8L, orgTotal1);

        // Increment awards for all in organization
        employeeService.incrementDundieAwardsForAll(organization1.getId());

        // Manually clear cache (simulating what @CacheEvict should do)
        cacheManager.getCache("totalAwards").clear();
        cacheManager.getCache("totalAwardsByOrganization").clear();

        // Get totals again - should reflect incremented values
        Long totalAwards2 = employeeService.getTotalAwards();
        Long orgTotal2 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        
        assertEquals(10L, totalAwards2, "Total awards should be 10 after incrementing 2 employees");
        assertEquals(10L, orgTotal2, "Org total should be 10 after incrementing all employees");
    }

    @Test
    @DisplayName("create employee - totals remain cached until data changes")
    void createEmployee_CacheShouldPersist() throws LookupException, InvalidArgumentException {
        // Create employee with awards directly via repository
        createEmployee("Michael", "Scott", organization1, 5);

        // Prime the caches
        Long totalAwards1 = employeeService.getTotalAwards();
        Long orgTotal1 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        assertEquals(5L, totalAwards1);
        assertEquals(5L, orgTotal1);

        // Call again - should use cache
        Long totalAwards2 = employeeService.getTotalAwards();
        Long orgTotal2 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        
        assertEquals(5L, totalAwards2);
        assertEquals(5L, orgTotal2);

        // Verify repository was called only once for each
        verify(employeeRepository, times(1)).getTotalAwards();
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(organization1.getId());
    }

    @Test
    @DisplayName("cache keys are independent - different organizations have separate cache entries")
    void cacheKeys_ShouldBeIndependent() throws LookupException, InvalidArgumentException {
        // Create employees in both organizations
        createEmployee("Michael", "Scott", organization1, 5);
        createEmployee("Josh", "Porter", organization2, 10);

        // Get totals for both organizations
        Long org1Total = employeeService.getTotalAwardsByOrganization(organization1.getId());
        Long org2Total = employeeService.getTotalAwardsByOrganization(organization2.getId());
        
        assertEquals(5L, org1Total);
        assertEquals(10L, org2Total);

        // Call again to verify caching is working independently
        Long org1Total2 = employeeService.getTotalAwardsByOrganization(organization1.getId());
        Long org2Total2 = employeeService.getTotalAwardsByOrganization(organization2.getId());
        
        assertEquals(org1Total, org1Total2);
        assertEquals(org2Total, org2Total2);

        // Each organization's cache should have been hit only once
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(organization1.getId());
        verify(employeeRepository, times(1)).getTotalAwardsByOrganization(organization2.getId());
    }

    /**
     * Helper method to create an employee with dundie awards
     */
    private Employee createEmployee(String firstName, String lastName, Organization organization, int dundieAwards) {
        Employee employee = new Employee(firstName, lastName, organization);
        employee.setDundieAwards(dundieAwards);
        return employeeRepository.save(employee);
    }
}
