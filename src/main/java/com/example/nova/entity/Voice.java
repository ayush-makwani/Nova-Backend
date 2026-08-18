package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** A catalog entry for an AI voice a companion can speak with (Atlas, Lyra, ...). */
@Entity
@Table(name = "voices", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    // Short descriptors shown next to the name, e.g. ["Deep", "Authoritative"].
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "voice_traits", joinColumns = @JoinColumn(name = "voice_id"))
    @OrderColumn(name = "trait_order")
    @Column(name = "trait", length = 50)
    @Builder.Default
    private List<String> traits = new ArrayList<>();

    @Column(nullable = false, length = 255)
    private String description;

    // Nullable: no audio sample uploaded yet, "play" is inert until this is set.
    @Column(name = "preview_audio_url", length = 255)
    private String previewAudioUrl;

    // Controls list ordering on the "AI Voice" screen.
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    // False = retired from the catalog; existing companions keep it, new selections can't pick it.
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
