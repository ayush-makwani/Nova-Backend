package com.example.nova;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SecureAppApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context (security config, JPA, etc.) wires up correctly.
    }
}
