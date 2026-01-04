package com.ninjaone.dundie_awards;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.exceptions.LookupException;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.OrganizationInfo;
import com.ninjaone.dundie_awards.services.EmployeeService;
import com.ninjaone.dundie_awards.services.OrganizationService;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataLoader implements CommandLineRunner {

    @Value("${DUNDIE_INIT_TEST_DATA:false}")
    private boolean testInitData;

    private final EmployeeService employeeService;
    
    private final OrganizationService organizationService;
    
    private final TransactionTemplate transactionTemplate;

    public DataLoader(EmployeeService employeeService, OrganizationService organizationService, TransactionTemplate transactionTemplate) {
        this.employeeService = employeeService;
        this.organizationService = organizationService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(String... args) {
        // uncomment to reseed data
        // employeeRepository.deleteAll();
        // organizationRepository.deleteAll();

        // Only add data if the flag is set and there are no employees
        if (testInitData && employeeService.getEmployeeCount() == 0) {
            transactionTemplate.execute(status -> {
                try {
                    OrganizationInfo organizationPikashu = OrganizationInfo.builder().name("Pikashu").build();
                    organizationPikashu = organizationService.save(organizationPikashu);
                    employeeService.save(EmployeeInfo.builder().firstName("John").lastName("Doe").organization(organizationPikashu).build());
                    employeeService.save(EmployeeInfo.builder().firstName("Jane").lastName("Smith").organization(organizationPikashu).build());
                    employeeService.save(EmployeeInfo.builder().firstName("Creed").lastName("Braton").organization(organizationPikashu).build());
        
                    OrganizationInfo organizationSquanchy = OrganizationInfo.builder().name("Squanchy").build();
                    organizationSquanchy = organizationService.save(organizationSquanchy);
        
                    employeeService.save(EmployeeInfo.builder().firstName("Michael").lastName("Scott").organization(organizationSquanchy).build());
                    employeeService.save(EmployeeInfo.builder().firstName("Dwight").lastName("Schrute").organization(organizationSquanchy).build());
                    employeeService.save(EmployeeInfo.builder().firstName("Jim").lastName("Halpert").organization(organizationSquanchy).build());
                    employeeService.save(EmployeeInfo.builder().firstName("Pam").lastName("Beesley").organization(organizationSquanchy).build());
                    log.info("Test data initialized");

                } catch (LookupException | InvalidArgumentException | NullPointerException e) {
                    log.error("Error saving organization: {}", e.getMessage());
                    status.setRollbackOnly();
                }
                return null;
            });
        }    
    }
}
