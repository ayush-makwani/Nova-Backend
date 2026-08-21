package com.example.nova.service;

import com.example.nova.dto.NotificationPreferencesResponse;
import com.example.nova.dto.UpdateNotificationPreferencesRequest;
import com.example.nova.entity.NotificationPreferences;
import com.example.nova.entity.User;
import com.example.nova.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository notificationPreferencesRepository;

    /** Settings > Notifications screen. Row is created with the default toggle values on first access. */
    @Transactional
    public NotificationPreferencesResponse getPreferences(User user) {
        return toResponse(getOrCreate(user));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(User user, UpdateNotificationPreferencesRequest request) {
        NotificationPreferences preferences = getOrCreate(user);

        preferences.setMeetingStartReminder(request.isMeetingStartReminder());
        preferences.setCompanionJoinedMeeting(request.isCompanionJoinedMeeting());
        preferences.setMeetingEnded(request.isMeetingEnded());
        preferences.setMomDelivered(request.isMomDelivered());
        preferences.setNewActionItemsCreated(request.isNewActionItemsCreated());
        preferences.setActionItemDueSoon(request.isActionItemDueSoon());
        preferences.setCompanionOfflineAlert(request.isCompanionOfflineAlert());
        preferences.setWeeklyUsageDigest(request.isWeeklyUsageDigest());

        preferences = notificationPreferencesRepository.save(preferences);
        log.info("User '{}' updated notification preferences", user.getUsername());

        return toResponse(preferences);
    }

    private NotificationPreferences getOrCreate(User user) {
        return notificationPreferencesRepository.findByUser(user)
                .orElseGet(() -> notificationPreferencesRepository.save(
                        NotificationPreferences.builder().user(user).build()));
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferences preferences) {
        return NotificationPreferencesResponse.builder()
                .meetingStartReminder(preferences.isMeetingStartReminder())
                .companionJoinedMeeting(preferences.isCompanionJoinedMeeting())
                .meetingEnded(preferences.isMeetingEnded())
                .momDelivered(preferences.isMomDelivered())
                .newActionItemsCreated(preferences.isNewActionItemsCreated())
                .actionItemDueSoon(preferences.isActionItemDueSoon())
                .companionOfflineAlert(preferences.isCompanionOfflineAlert())
                .weeklyUsageDigest(preferences.isWeeklyUsageDigest())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
