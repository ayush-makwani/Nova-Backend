package com.example.nova.repository;

import com.example.nova.entity.Project;
import com.example.nova.entity.ProjectMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMemoryRepository extends JpaRepository<ProjectMemory, Long> {
    Optional<ProjectMemory> findByProject(Project project);
}
