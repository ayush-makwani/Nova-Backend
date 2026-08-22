package com.example.nova.config;

import com.example.nova.entity.Voice;
import com.example.nova.repository.VoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Seeds the built-in voice catalog on first startup; leaves it alone once any voice exists. */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceCatalogSeeder  // implements CommandLineRunner
 {

    private final VoiceRepository voiceRepository;
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        if (voiceRepository.count() > 0) {
//            return;
//        }
//
//        List<Voice> defaults = List.of(
//                Voice.builder()
//                        .name("Atlas")
//                        .traits(List.of("Deep", "Authoritative"))
//                        .description("Calm, confident baritone - ideal for executive meetings and formal briefings.")
//                        .displayOrder(1)
//                        .active(true)
//                        .build(),
//                Voice.builder()
//                        .name("Lyra")
//                        .traits(List.of("Clear", "Warm"))
//                        .description("Bright, conversational tone - natural for team standups and collaborative sessions.")
//                        .displayOrder(2)
//                        .active(true)
//                        .build(),
//                Voice.builder()
//                        .name("Orion")
//                        .traits(List.of("Neutral", "Professional"))
//                        .description("Steady mid-range voice - versatile across all meeting types and languages.")
//                        .displayOrder(3)
//                        .active(true)
//                        .build(),
//                Voice.builder()
//                        .name("Nova")
//                        .traits(List.of("Energetic", "Precise"))
//                        .description("Crisp, upbeat delivery - great for sales calls, demos, and client-facing meetings.")
//                        .displayOrder(4)
//                        .active(true)
//                        .build()
//        );
//
//        voiceRepository.saveAll(defaults);
//        log.info("Seeded {} default AI voices", defaults.size());
//    }
}
