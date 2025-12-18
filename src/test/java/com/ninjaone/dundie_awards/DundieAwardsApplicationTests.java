package com.ninjaone.dundie_awards;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.ninjaone.dundie_awards.model.ActivityInfo;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.cloud.stream.binding.out=activity-out-0"
})

public class DundieAwardsApplicationTests {

	@Value("${spring.cloud.stream.binding.out}")
	private String activityBindingName;

	@Container
    public static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private StreamBridge streamBridge;


    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @Test
    public void kafkaShouldBeRunning() {
        assertTrue(kafka.isRunning());
    }

	@Test
	public void testSendMessage() {
		ActivityInfo activityInfo = ActivityInfo.builder().event("TEST_EVENT").occuredAt(LocalDateTime.now()).build();
		streamBridge.send(activityBindingName, activityInfo);
	}

}
