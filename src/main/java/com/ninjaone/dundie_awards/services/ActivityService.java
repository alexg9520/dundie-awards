package com.ninjaone.dundie_awards.services;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.ninjaone.dundie_awards.exceptions.InvalidArgumentException;
import com.ninjaone.dundie_awards.model.Activity;
import com.ninjaone.dundie_awards.model.ActivityInfo;
import com.ninjaone.dundie_awards.repository.ActivityRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    /**
     * Find all activities
     * 
     * @return List<ActivityInfo> of all activities
     */
    // TODO: needs to support pagination
    public List<ActivityInfo> findAll() {
        List<Activity> activities = activityRepository.findAll();
        return activities.stream().map(this::createActivityInfoFromActivity).toList();
    }

    /** 
     * Save activity info to the repository
     * 
     * @param activityInfo the activity info to save
     * @return the saved Activity
     */
    public ActivityInfo save(ActivityInfo activityInfo) {
        checkForNullValue(activityInfo, "ActivityInfo is null", "No activity info provided");
        Activity newActivity = new Activity(activityInfo.occuredAt(), activityInfo.event());
        return createActivityInfoFromActivity(activityRepository.save(newActivity));
    }

    /**
     * Create ActivityInfo from Activity
     * 
     * @param employee the Employee entity
     * @return ActivityInfo record
     */
    public ActivityInfo createActivityInfoFromActivity(Activity activity) {
        checkForNullValue(activity, "Activity is null", "No activity was provided");
        ActivityInfo activityInfo = ActivityInfo.builder()
                .id(activity.getId())
                .occuredAt(activity.getOccuredAt())
                .event(activity.getEvent())
                .build();
        return activityInfo;
    }

    /** Message broker consumer bean to process incoming ActivityInfo messages */
    @Bean
    public Consumer<Message<ActivityInfo>> activity() {
        return event -> {
            try {
                Acknowledgment ack = event.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
                ActivityInfo activityInfo = event.getPayload();

                // Save activity info to the repository
                save(activityInfo);

                // Acknowledge the message after successful processing
                ack.acknowledge();
            } catch (Exception ex) {
                // Log the error and do not acknowledge to trigger retry or DLQ
                log.error("Error processing activity info: {}", ex.getMessage(), ex);
            }

        };
    }

    private void checkForNullValue(Object obj, String logMessage, String errorMessage) {
        if (obj == null) {
            log.error(logMessage);
            throw new InvalidArgumentException(errorMessage);
        }
    }    
}
