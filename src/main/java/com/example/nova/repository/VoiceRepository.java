package com.example.nova.repository;

import com.example.nova.entity.Voice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoiceRepository extends JpaRepository<Voice, Long> {
    List<Voice> findAllByActiveTrueOrderByDisplayOrderAsc();
    Optional<Voice> findByNameIgnoreCase(String name);
}
