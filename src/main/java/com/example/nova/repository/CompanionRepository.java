package com.example.nova.repository;

import com.example.nova.entity.Companion;
import com.example.nova.entity.Company;
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

    /**
     * Companions the user owns/purchased (user_id) plus whichever companion
     * is paired with them via the Team Users screen (assigned_user_id) - a
     * team member who never bought anything themselves still needs to see the
     * one companion assigned to them.
     */
    List<Companion> findAllByUserOrAssignedUserOrderByCreatedAtAsc(User user, User assignedUser);

    /** Scoped lookup so one user can never read or modify another user's companion. */
    Optional<Companion> findByIdAndUser(Long id, User user);

    /** The companion a project is assigned to, if any. */
    Optional<Companion> findByProject(Project project);

    /** Oldest companion not yet linked to any project - used to auto-assign a companion when a project is created. */
    Optional<Companion> findFirstByUserAndProjectIsNullOrderBySeatNumberAsc(User user);

    /** The companion a team member is paired with, if any (Team Users screen). */
    Optional<Companion> findByAssignedUser(User assignedUser);

    /** Every companion owned across a company (by whoever purchased it) not yet paired with a team member. */
    List<Companion> findAllByUser_CompanyAndAssignedUserIsNullOrderBySeatNumberAsc(Company company);

    /** Scoped lookup so an admin can never assign a companion owned outside their own company. */
    Optional<Companion> findByIdAndUser_Company(Long id, Company company);
}
