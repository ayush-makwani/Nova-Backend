package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @OrderColumn(name = "tag_order")
    @Column(name = "tag", length = 30)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // Free-form context pasted in step 2 ("Add Context") for the companion to read before its first meeting.
    @Column(name = "text_context", length = 5000)
    private String textContext;

    // Placeholder until document/voice-note upload is implemented; a future context-file API increments this.
    @Column(name = "context_files_count", nullable = false)
    @Builder.Default
    private int contextFilesCount = 0;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
