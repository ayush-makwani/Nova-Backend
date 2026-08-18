package com.example.nova.service;

import com.example.nova.config.CompanionProperties;
import com.example.nova.dto.CompanionResponse;
import com.example.nova.dto.CompanionSettingsResponse;
import com.example.nova.dto.CreateCompanionRequest;
import com.example.nova.dto.CreateCompanionResponse;
import com.example.nova.dto.UpdateCompanionSettingsRequest;
import com.example.nova.dto.UpdateCompanionVoiceRequest;
import com.example.nova.dto.VoiceResponse;
import com.example.nova.entity.Companion;
import com.example.nova.entity.CompanionPresenceStatus;
import com.example.nova.entity.CompanionStatus;
import com.example.nova.entity.User;
import com.example.nova.entity.Voice;
import com.example.nova.exception.CompanionEmailAlreadyExistsException;
import com.example.nova.exception.CompanionNotFoundException;
import com.example.nova.exception.PaymentNotSupportedException;
import com.example.nova.exception.VoiceNotFoundException;
import com.example.nova.repository.CompanionRepository;
import com.example.nova.repository.VoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionService {

    private final CompanionRepository companionRepository;
    private final VoiceRepository voiceRepository;
    private final CompanionProperties companionProperties;

    @Transactional
    public CreateCompanionResponse createCompanions(User user, CreateCompanionRequest request) {
        List<String> emails = normalizeAndValidateEmails(request.getEmails());

        // Payment is not implemented yet. The flag defaults to false, which
        // means checkout skips payment entirely and companions go straight to
        // ACTIVE - matching the current UI, which stubs out step 3.
        if (companionProperties.getPayment().isEnabled()) {
            throw new PaymentNotSupportedException(
                    "Payment processing is not yet available; companions cannot be activated at this time");
        }

        Voice defaultVoice = voiceRepository.findByNameIgnoreCase(companionProperties.getDefaultVoice())
                .orElseThrow(() -> new IllegalStateException(
                        "Default voice '" + companionProperties.getDefaultVoice() + "' is not configured in the voice catalog"));

        long existingCount = companionRepository.countByUser(user);
        CompanionProperties.Behavior defaultBehavior = companionProperties.getDefaultBehavior();
        List<Companion> created = new ArrayList<>();
        for (int i = 0; i < emails.size(); i++) {
            int seatNumber = (int) (existingCount + i + 1);
            Companion companion = Companion.builder()
                    .user(user)
                    .name(companionProperties.getNamePrefix() + "-" + seatNumber)
                    .seatNumber(seatNumber)
                    .email(emails.get(i))
                    .voice(defaultVoice)
                    .status(CompanionStatus.ACTIVE)
                    .presenceStatus(CompanionPresenceStatus.IDLE)
                    .meetingsCount(0)
                    .autoJoinMeetings(defaultBehavior.isAutoJoinMeetings())
                    .sendMomAutomatically(defaultBehavior.isSendMomAutomatically())
                    .respondInVoice(defaultBehavior.isRespondInVoice())
                    .recordMeetingAudio(defaultBehavior.isRecordMeetingAudio())
                    .pricePerMonth(companionProperties.getPricePerMonth())
                    .build();
            created.add(companionRepository.save(companion));
        }

        log.info("User '{}' activated {} companion(s)", user.getUsername(), created.size());

        BigDecimal total = companionProperties.getPricePerMonth().multiply(BigDecimal.valueOf(created.size()));
        String message = created.size() + " Companion" + (created.size() == 1 ? "" : "s") + " activated!";

        return CreateCompanionResponse.builder()
                .message(message)
                .companions(created.stream().map(this::toResponse).collect(Collectors.toList()))
                .totalMonthlyPrice(total)
                .build();
    }

    public List<CompanionResponse> listCompanions(User user) {
        return companionRepository.findAllByUserOrderByCreatedAtAsc(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CompanionSettingsResponse getSettings(User user, Long companionId) {
        Companion companion = companionRepository.findByIdAndUser(companionId, user)
                .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));
        return toSettingsResponse(companion);
    }

    @Transactional
    public CompanionSettingsResponse updateSettings(User user, Long companionId, UpdateCompanionSettingsRequest request) {
        Companion companion = companionRepository.findByIdAndUser(companionId, user)
                .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));

        String email = request.getEmail().trim().toLowerCase();
        if (!email.equals(companion.getEmail()) && companionRepository.existsByEmailAndIdNot(email, companionId)) {
            throw new CompanionEmailAlreadyExistsException("Email is already assigned to a companion: " + email);
        }

        companion.setName(request.getDisplayName().trim());
        companion.setEmail(email);
        companion.setAutoJoinMeetings(request.isAutoJoinMeetings());
        companion.setSendMomAutomatically(request.isSendMomAutomatically());
        companion.setRespondInVoice(request.isRespondInVoice());
        companion.setRecordMeetingAudio(request.isRecordMeetingAudio());

        companionRepository.save(companion);
        log.info("User '{}' updated settings for companion {}", user.getUsername(), companionId);

        return toSettingsResponse(companion);
    }

    public List<VoiceResponse> listVoices(User user, Long companionId) {
        Companion companion = companionRepository.findByIdAndUser(companionId, user)
                .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));
        return voiceRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(voice -> toVoiceResponse(voice, voice.getId().equals(companion.getVoice().getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<VoiceResponse> updateVoice(User user, Long companionId, UpdateCompanionVoiceRequest request) {
        Companion companion = companionRepository.findByIdAndUser(companionId, user)
                .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));

        Voice voice = voiceRepository.findById(request.getVoiceId())
                .filter(Voice::isActive)
                .orElseThrow(() -> new VoiceNotFoundException("Voice not found"));

        companion.setVoice(voice);
        companionRepository.save(companion);
        log.info("User '{}' set companion {} voice to '{}'", user.getUsername(), companionId, voice.getName());

        return listVoices(user, companionId);
    }

    private List<String> normalizeAndValidateEmails(List<String> rawEmails) {
        Set<String> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String raw : rawEmails) {
            String email = raw.trim().toLowerCase();
            if (!seen.add(email)) {
                throw new CompanionEmailAlreadyExistsException("Duplicate email in request: " + email);
            }
            if (companionRepository.existsByEmail(email)) {
                throw new CompanionEmailAlreadyExistsException("Email is already assigned to a companion: " + email);
            }
            normalized.add(email);
        }
        return normalized;
    }

    private CompanionResponse toResponse(Companion companion) {
        return CompanionResponse.builder()
                .id(companion.getId())
                .name(companion.getName())
                .seatNumber(companion.getSeatNumber())
                .email(companion.getEmail())
                .voice(companion.getVoice().getName())
                .project(companion.getProject() != null ? companion.getProject().getName() : null)
                .status(companion.getStatus())
                .presenceStatus(companion.getPresenceStatus())
                .meetingsCount(companion.getMeetingsCount())
                .lastMeetingAt(companion.getLastMeetingAt())
                .autoJoinMeetings(companion.isAutoJoinMeetings())
                .sendMomAutomatically(companion.isSendMomAutomatically())
                .respondInVoice(companion.isRespondInVoice())
                .recordMeetingAudio(companion.isRecordMeetingAudio())
                .pricePerMonth(companion.getPricePerMonth())
                .createdAt(companion.getCreatedAt())
                .build();
    }

    private CompanionSettingsResponse toSettingsResponse(Companion companion) {
        return CompanionSettingsResponse.builder()
                .id(companion.getId())
                .displayName(companion.getName())
                .email(companion.getEmail())
                .autoJoinMeetings(companion.isAutoJoinMeetings())
                .sendMomAutomatically(companion.isSendMomAutomatically())
                .respondInVoice(companion.isRespondInVoice())
                .recordMeetingAudio(companion.isRecordMeetingAudio())
                .build();
    }

    private VoiceResponse toVoiceResponse(Voice voice, boolean selected) {
        return VoiceResponse.builder()
                .id(voice.getId())
                .name(voice.getName())
                .traits(voice.getTraits())
                .description(voice.getDescription())
                .previewAudioUrl(voice.getPreviewAudioUrl())
                .selected(selected)
                .build();
    }
}
