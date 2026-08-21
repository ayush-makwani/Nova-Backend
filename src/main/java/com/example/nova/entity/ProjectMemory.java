package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The compact, AI-maintained summary of a project's meetings so far - what
 * gets fed back to the model as context. One row per project, replaced
 * wholesale (never merged) each time the client folds a new meeting in.
 */
@Entity
@Table(name = "project_memory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    // Required but may be empty - the prose bulk of what the model is given.
    @Column(name = "compact_memory", length = 20000, nullable = false)
    @Builder.Default
    private String compactMemory = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_memory_decisions", joinColumns = @JoinColumn(name = "project_memory_id"))
    @OrderColumn(name = "decision_order")
    @Column(name = "decision", length = 2000)
    @Builder.Default
    private List<String> decisions = new ArrayList<>();

    // Flat strings in a fixed display format, e.g. "Name: description (due YYYY-MM-DD) [PRIORITY]".
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_memory_actions", joinColumns = @JoinColumn(name = "project_memory_id"))
    @OrderColumn(name = "action_order")
    @Column(name = "action", length = 2000)
    @Builder.Default
    private List<String> actions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_memory_notes", joinColumns = @JoinColumn(name = "project_memory_id"))
    @OrderColumn(name = "note_order")
    @Column(name = "note", length = 2000)
    @Builder.Default
    private List<String> notes = new ArrayList<>();

    // How many meetings have been folded in so far. A display number, not a
    // strict count - see ProjectMemoryService.replaceMemory for why drift is acceptable.
    @Column(name = "meetings_summarised", nullable = false)
    @Builder.Default
    private int meetingsSummarised = 0;

    // Null until the first write.
    @Column(name = "updated_at")
    private Instant updatedAt;
}
