package com.example.nova.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "app.companions")
public class CompanionProperties {

    /** Monthly price charged per companion (used to compute totals and stamped onto each created companion). */
    private BigDecimal pricePerMonth = new BigDecimal("89.00");

    /** Upper bound on how many companions can be requested in a single create call. */
    private int maxQuantity = 10;

    /** Prefix used when auto-generating a companion's display name, e.g. "NOVA-4". */
    private String namePrefix = "NOVA";

    /** Voice assigned to a companion at creation time; changeable later from the companion detail page. */
    private String defaultVoice = "Aoede";

    private Behavior defaultBehavior = new Behavior();

    private Payment payment = new Payment();

    /** Default behavior toggles stamped onto a companion at creation time; editable later from Companion Settings. */
    @Data
    public static class Behavior {
        private boolean autoJoinMeetings = true;
        private boolean sendMomAutomatically = true;
        private boolean respondInVoice = true;
        private boolean recordMeetingAudio = false;
    }

    @Data
    public static class Payment {
        /**
         * Master on/off switch for real payment processing. When false (the
         * default), checkout skips payment entirely and companions are
         * activated immediately on creation - this is the current behaviour
         * since no payment provider is integrated yet. When true, creation
         * requests are rejected (501) until payment processing is implemented.
         */
        private boolean enabled = false;
    }
}
