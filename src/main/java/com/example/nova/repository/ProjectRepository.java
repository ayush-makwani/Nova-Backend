package com.example.nova.repository;

import com.example.nova.entity.Project;
import com.example.nova.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByUserAndNameIgnoreCase(User user, String name);
    List<Project> findAllByUserOrderByCreatedAtAsc(User user);

    /** Scoped lookup so one user can never read or reference another user's project. */
    Optional<Project> findByIdAndUser(Long id, User user);
}
