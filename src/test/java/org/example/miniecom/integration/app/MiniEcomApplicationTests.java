package org.example.miniecom.integration.app;

import org.example.miniecom.integration.support.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MiniEcomApplicationTests extends TestcontainersConfig {

    @Test
    void contextLoads() {
    }

}
