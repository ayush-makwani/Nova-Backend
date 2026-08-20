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

    // S3 object keys for documents uploaded in step 2 ("Add Context") - not
    // full URLs; resolve against AwsCredential's bucket/url when the actual
    // file is needed. The upload itself happens elsewhere (out of scope here);
    // this just links already-uploaded keys to the project at creation time.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_document_keys", joinColumns = @JoinColumn(name = "project_id"))
    @OrderColumn(name = "key_order")
    @Column(name = "document_key", length = 500)
    @Builder.Default
    private List<String> documentKeys = new ArrayList<>();

    // Nullable: S3 object key for the recorded voice-note overview, if any (same "not a full URL" caveat as above).
    @Column(name = "voice_note_key", length = 500)
    private String voiceNoteKey;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
