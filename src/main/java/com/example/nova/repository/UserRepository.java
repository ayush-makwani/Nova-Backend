package com.example.nova.repository;

import com.example.nova.entity.Company;
import com.example.nova.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Looks up a user linked to a specific SAML identity provider account. */
    Optional<User> findBySsoRegistrationIdAndSsoSubjectId(String ssoRegistrationId, String ssoSubjectId);

    /** Every member of a company workspace (Team Users screen). */
    List<User> findAllByCompanyOrderByCreatedAtAsc(Company company);
}
