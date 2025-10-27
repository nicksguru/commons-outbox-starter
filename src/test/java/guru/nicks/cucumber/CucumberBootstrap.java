package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;

import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Initializes Spring Context shared by all scenarios. Mocking is done inside step definition classes to let them
 * program a different behavior. However, purely default mocks can be declared here (using annotations), but remember to
 * not alter their behavior in step classes.
 * <p>
 * Please keep in mind that mocked Spring beans ({@link MockitoBean @MockitoBean}) declared in step definition classes
 * conflict with each other because all the steps are part of the same test suite i.e. Spring context. POJO mocks
 * ({@link Mock @Mock}) do not conflict with each other.
 */
@CucumberContextConfiguration
@ContextConfiguration(classes = {
        // scenario-scoped states
        TextWorld.class
})
public class CucumberBootstrap {
}
