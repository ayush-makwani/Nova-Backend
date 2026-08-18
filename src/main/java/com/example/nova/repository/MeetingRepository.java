package com.example.nova.repository;

import com.example.nova.entity.Companion;
import com.example.nova.entity.Meeting;
import com.example.nova.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findAllByCompanionOrderByScheduledAtDesc(Companion companion);
    List<Meeting> findAllByCompanionAndProjectIgnoreCaseOrderByScheduledAtDesc(Companion companion, String project);
    long countByCompanion(Companion companion);
    Optional<Meeting> findFirstByCompanionOrderByScheduledAtDesc(Companion companion);

    /** All of a user's meetings (across every companion) scheduled within [start, end) - for the calendar view. */
    List<Meeting> findAllByCompanion_UserAndScheduledAtGreaterThanEqualAndScheduledAtLessThanOrderByScheduledAtAsc(
            User user, Instant start, Instant end);
}
