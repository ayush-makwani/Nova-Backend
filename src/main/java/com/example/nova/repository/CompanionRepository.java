package com.example.nova.repository;

import com.example.nova.entity.Companion;
import com.example.nova.entity.Project;
import com.example.nova.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanionRepository extends JpaRepository<Companion, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    long countByUser(User user);
    List<Companion> findAllByUserOrderByCreatedAtAsc(User user);

    /** Scoped lookup so one user can never read or modify another user's companion. */
    Optional<Companion> findByIdAndUser(Long id, User user);

    /** The companion a project is assigned to, if any. */
    Optional<Companion> findByProject(Project project);

    /** Oldest companion not yet linked to any project - used to auto-assign a companion when a project is created. */
    Optional<Companion> findFirstByUserAndProjectIsNullOrderBySeatNumberAsc(User user);
}
