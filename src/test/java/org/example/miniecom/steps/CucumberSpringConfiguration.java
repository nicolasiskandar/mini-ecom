package org.example.miniecom.steps;

import io.cucumber.spring.CucumberContextConfiguration;
import org.example.miniecom.integration.support.TestcontainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class CucumberSpringConfiguration extends TestcontainersConfig {
}
