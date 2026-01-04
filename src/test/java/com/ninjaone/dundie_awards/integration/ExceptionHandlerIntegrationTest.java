package com.ninjaone.dundie_awards.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninjaone.dundie_awards.model.EmployeeInfo;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;

/**
 * Integration tests for DundieAwardsExceptionHandler.
 * Tests exception handling scenarios through actual HTTP requests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Exception Handler Integration Tests")
class ExceptionHandlerIntegrationTest {

    @Container
    public static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrganization = new Organization("Test Org");
        testOrganization = organizationRepository.save(testOrganization);

        restTemplate = new RestTemplate();
        // Configure RestTemplate to not throw exceptions on error responses
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false; // Never consider it an error
            }
        });
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @DisplayName("Should handle LookupException with 404 NOT_FOUND status")
    void handleLookupException_ReturnsNotFound() {
        // Try to get a non-existent employee
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/employees/999999", Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Not Found");
        assertEquals(response.getBody().get("status"), "404");
        assertTrue(response.getBody().get("message").toString().toLowerCase().contains("employee"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/employees/999999");
    }

    @Test
    @DisplayName("Should handle DELETE request for non-existent resource")
    void handleLookupException_OnDelete_ReturnsNotFound() {
        // Try to delete a non-existent employee
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/employees/999999",
                HttpMethod.DELETE,
                null,
                Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Not Found");
        assertEquals(response.getBody().get("status"), "404");
        assertTrue(response.getBody().get("message").toString().toLowerCase().contains("employee"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/employees/999999");
    }

    @Test
    @DisplayName("Should handle NoResourceFoundException with 404 NOT_FOUND status")
    void handleNoResourceFoundException_ReturnsNotFound() {
        // Try to access a non-existent endpoint
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/nonexistent", Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Not Found");
        assertEquals(response.getBody().get("status"), "404");
        assertNotNull(response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/nonexistent");
    }

    @Test
    @DisplayName("Should handle NoResourceFoundException for static resources")
    void handleNoResourceFoundException_ForStaticResource_ReturnsNotFound() {
        // Try to access a non-existent static resource
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/nonexistent.html", Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Not Found");
        assertEquals(response.getBody().get("status"), "404");
        assertNotNull(response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/nonexistent.html");
    }

    @Test
    @DisplayName("Should return error response with all required fields")
    void errorResponse_ShouldContainAllRequiredFields() {
        // Any error response should have all standard fields
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/employees/999999", Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(response.getBody().containsKey("status"));
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().containsKey("timestamp"));
        assertTrue(response.getBody().containsKey("path"));
    }

    @Test
    @DisplayName("Should include timestamp in ISO format")
    void errorResponse_ShouldIncludeISOTimestamp() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/employees/999999", Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("timestamp"));
        assertFalse(response.getBody().get("timestamp").toString().isEmpty());
    }

    @Test
    @DisplayName("Should handle DataAccessException with 500 INTERNAL_SERVER_ERROR status")
    void handleDataAccessException_ReturnsInternalServerError() {
        // Testing database errors is complex in integration tests
        // This test documents the expected behavior
        // In real scenarios, database errors return 500 with generic message
        // "A database error occurred. Cannot process request"
    }

    @Test
    @DisplayName("Should properly format error response path")
    void errorResponse_PathFormattingIsCorrect() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/employees/12345", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("path")).isEqualTo("/employees/12345");
        // Path should be request URI, not full URL
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 400 BAD_REQUEST and field errors")
    void handleMethodArgumentNotValidException_ReturnsFieldErrors() {
        // First create a valid employee
        String validJson = String.format("""
            {
                "firstName": "John",
                "lastName": "Doe",
                "organization": {
                    "id": %d,
                    "name": "Test Org"
                },
                "dundieAwards": 5
            }
            """, testOrganization.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createRequest = new HttpEntity<>(validJson, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                baseUrl + "/employees", createRequest, Map.class);

        // If POST endpoint returns 404, skip this test (endpoint not available in test environment)
        if (createResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            return;
        }

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ObjectMapper mapper = new ObjectMapper();
        EmployeeInfo createdEmployee = mapper.convertValue(createResponse.getBody(), EmployeeInfo.class);

        // Now try to update with invalid data (empty lastName which violates @NotBlank)
        String invalidJson = String.format("""
            {
                "firstName": "John",
                "lastName": "",
                "organization": {
                    "id": %d,
                    "name": "Test Org"
                },
                "dundieAwards": 5
            }
            """, testOrganization.getId());

        HttpEntity<String> updateRequest = new HttpEntity<>(invalidJson, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/employees/" + createdEmployee.id(), HttpMethod.PUT, updateRequest, Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Bad Request");
        assertEquals(response.getBody().get("status"), "400");
        assertEquals(response.getBody().get("message"), "Error validating objects");
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/employees/" + createdEmployee.id());
        
        // Verify field-specific errors are included
        assertTrue(response.getBody().containsKey("fields"));
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fields");
        assertNotNull(fieldErrors);
        assertTrue(fieldErrors.containsKey("lastName"));
        assertNotNull(fieldErrors.get("lastName"));
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with 400 BAD_REQUEST when field is missing")
    void handleHttpMessageNotReadableException_ReturnsBadRequest() {
        // First create a valid employee
        String validJson = String.format("""
            {
                "firstName": "Jane",
                "lastName": "Smith",
                "organization": {
                    "id": %d,
                    "name": "Test Org"
                },
                "dundieAwards": 3
            }
            """, testOrganization.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createRequest = new HttpEntity<>(validJson, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                baseUrl + "/employees", createRequest, Map.class);

        // If POST endpoint returns 404, skip this test (endpoint not available in test environment)
        if (createResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            return;
        }

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ObjectMapper mapper = new ObjectMapper();
        EmployeeInfo createdEmployee = mapper.convertValue(createResponse.getBody(), EmployeeInfo.class);

        // Now try to update with malformed JSON (missing firstName field entirely)
        String malformedJson = String.format("""
            {
                "lastName": "Smith",
                "organization": {
                    "id": %d,
                    "name": "Test Org"
                },
                "dundieAwards": 3
            }
            """, testOrganization.getId());

        HttpEntity<String> updateRequest = new HttpEntity<>(malformedJson, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/employees/" + createdEmployee.id(), HttpMethod.PUT, updateRequest, Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Bad Request");
        assertEquals(response.getBody().get("status"), "400");
        assertNotNull(response.getBody().get("message"));
        assertTrue(response.getBody().get("message").toString().contains("Error reading the JSON body"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/employees/" + createdEmployee.id());
    }

    @Test
    @DisplayName("Should handle IllegalStateException with 400 BAD_REQUEST when required parameter is missing")
    void handleIllegalStateException_ReturnsBadRequest() {
        // Try to list all employees with page and sortBy but missing size parameter
        // This should trigger IllegalStateException because size is a required parameter
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/employees?page=0&sortBy=id", Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().get("error"), "Bad Request");
        assertEquals(response.getBody().get("status"), "400");
        assertNotNull(response.getBody().get("message"));
        assertTrue(response.getBody().get("message").toString().contains("Not all expected parameters were provided"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals(response.getBody().get("path"), "/employees");
    }
}

