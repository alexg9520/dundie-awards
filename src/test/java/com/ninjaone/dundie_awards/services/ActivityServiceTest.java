package com.ninjaone.dundie_awards.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.model.ActivityInfo;
import com.ninjaone.dundie_awards.repository.ActivityRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Activity Service Tests")
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityService activityService;

    private Activity testActivity;
    
    private ActivityInfo testActivityInfo;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        testActivity = new Activity(now, "DUNDIE_AWARDS_INCREMENTED: 5");
        
        testActivityInfo = ActivityInfo.builder()
                .id(1L)
                .occuredAt(now)
                .event("DUNDIE_AWARDS_INCREMENTED: 5")
                .build();
    }

    @Test
    @DisplayName("findAll - should return all activities")
    void findAll_ShouldReturnAllActivities() {
        // Arrange
        Activity activity2 = new Activity(LocalDateTime.now(), "EMPLOYEE_CREATED");
        when(activityRepository.findAll()).thenReturn(Arrays.asList(testActivity, activity2));

        // Act
        List<ActivityInfo> result = activityService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertEquals("DUNDIE_AWARDS_INCREMENTED: 5", result.get(0).event());
        assertEquals("EMPLOYEE_CREATED", result.get(1).event());
        verify(activityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("save - should save activity successfully")
    void save_WithValidActivityInfo_ShouldSaveActivity() {
        // Arrange
        ActivityInfo newActivityInfo = ActivityInfo.builder()
                .occuredAt(LocalDateTime.now())
                .event("TEST_EVENT")
                .build();

        Activity savedActivity = new Activity(newActivityInfo.occuredAt(), newActivityInfo.event());
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // Act
        ActivityInfo result = activityService.save(newActivityInfo);

        // Assert
        assertNotNull(result);
        assertEquals(result.event(),"TEST_EVENT");
        verify(activityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    @DisplayName("save - should throw exception when activity info is null")
    void save_WhenActivityInfoIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> activityService.save(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No activity info provided");

        verify(activityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createActivityInfoFromActivity - should convert activity to info")
    void createActivityInfoFromActivity_ShouldConvertActivity() {
        // Act
        ActivityInfo result = activityService.createActivityInfoFromActivity(testActivity);

        // Assert
        assertNotNull(result);
        // The ID should be different since it's generated upon saving
        assertNotEquals(testActivityInfo.id(), result.id());
        assertEquals(result.event(),"DUNDIE_AWARDS_INCREMENTED: 5");
        assertNotNull(result.occuredAt());
    }

    @Test
    @DisplayName("createActivityInfoFromActivity - should throw exception when activity is null")
    void createActivityInfoFromActivity_WhenActivityIsNull_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> activityService.createActivityInfoFromActivity(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("No activity was provided");
    }

    @Test
    @DisplayName("activity consumer - should process message and acknowledge")
    void activityConsumer_WithValidMessage_ShouldProcessAndAcknowledge() {
        // Arrange
        Acknowledgment mockAck = mock(Acknowledgment.class);
        
        ActivityInfo activityInfo = ActivityInfo.builder()
                .occuredAt(LocalDateTime.now())
                .event("TEST_EVENT")
                .build();

        Message<ActivityInfo> message = MessageBuilder
                .withPayload(activityInfo)
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, mockAck)
                .build();

        Activity savedActivity = new Activity(activityInfo.occuredAt(), activityInfo.event());
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // Get the consumer bean
        Consumer<Message<ActivityInfo>> consumer = activityService.activity();

        // Act
        consumer.accept(message);

        // Assert
        verify(activityRepository, times(1)).save(any(Activity.class));
        verify(mockAck, times(1)).acknowledge();
    }

    @Test
    @DisplayName("activity consumer - should not acknowledge on error")
    void activityConsumer_WhenErrorOccurs_ShouldNotAcknowledge() {
        // Arrange
        Acknowledgment mockAck = mock(Acknowledgment.class);
        
        ActivityInfo activityInfo = ActivityInfo.builder()
                .occuredAt(LocalDateTime.now())
                .event("TEST_EVENT")
                .build();

        Message<ActivityInfo> message = MessageBuilder
                .withPayload(activityInfo)
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, mockAck)
                .build();

        when(activityRepository.save(any(Activity.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Get the consumer bean
        Consumer<Message<ActivityInfo>> consumer = activityService.activity();

        // Act
        consumer.accept(message);

        // Assert
        verify(activityRepository, times(1)).save(any(Activity.class));
        verify(mockAck, never()).acknowledge();
    }

    @Test
    @DisplayName("activity consumer - should handle message without acknowledgment header")
    void activityConsumer_WithoutAckHeader_ShouldHandleGracefully() {
        // Arrange
        ActivityInfo activityInfo = ActivityInfo.builder()
                .occuredAt(LocalDateTime.now())
                .event("TEST_EVENT")
                .build();

        Message<ActivityInfo> message = MessageBuilder
                .withPayload(activityInfo)
                .build();

        Activity savedActivity = new Activity(activityInfo.occuredAt(), activityInfo.event());
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // Get the consumer bean
        Consumer<Message<ActivityInfo>> consumer = activityService.activity();

        // Act & Assert - Should not throw exception even without ack header
        consumer.accept(message);

        verify(activityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    @DisplayName("save - should preserve all activity info fields")
    void save_ShouldPreserveAllFields() {
        // Arrange
        LocalDateTime specificTime = LocalDateTime.of(2025, 12, 18, 10, 30);
        ActivityInfo activityInfo = ActivityInfo.builder()
                .occuredAt(specificTime)
                .event("SPECIFIC_EVENT")
                .build();

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        Activity savedActivity = new Activity(specificTime, "SPECIFIC_EVENT");
        when(activityRepository.save(activityCaptor.capture())).thenReturn(savedActivity);

        // Act
        ActivityInfo result = activityService.save(activityInfo);
        assertThat(activityInfo.equals(result));

        // Assert
        Activity capturedActivity = activityCaptor.getValue();
        assertThat(capturedActivity.getOccuredAt()).isEqualTo(specificTime);
        assertThat(capturedActivity.getEvent()).isEqualTo("SPECIFIC_EVENT");
    }
}
