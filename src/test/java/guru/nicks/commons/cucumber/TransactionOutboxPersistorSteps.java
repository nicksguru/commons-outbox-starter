package guru.nicks.commons.cucumber;

import guru.nicks.commons.outbox.config.CommonsOutboxAutoConfiguration;
import guru.nicks.commons.outbox.domain.TransactionOutboxProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gruelbox.transactionoutbox.DefaultPersistor;
import com.gruelbox.transactionoutbox.Persistor;
import com.gruelbox.transactionoutbox.jackson.TransactionOutboxJacksonModule;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.env.Environment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TransactionOutboxPersistorSteps {

    @Spy
    private ObjectMapper objectMapper;
    @Captor
    private ArgumentCaptor<TransactionOutboxJacksonModule> moduleCaptor;
    private AutoCloseable closeableMocks;

    private TransactionOutboxProperties properties;
    private CommonsOutboxAutoConfiguration config;
    private Persistor persistor;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("transaction outbox properties with Jackson serialization {booleanValue}")
    public void transactionOutboxPropertiesWithJacksonSerialization(boolean useJackson) {
        properties = TransactionOutboxProperties.builder()
                .useJackson(useJackson)
                .unblockBlockedTasks(false)
                .blockAfterAttempts(3)
                .backgroundJobInitialDelay(Duration.ofSeconds(10))
                .backgroundJobRestartDelay(Duration.ofMinutes(1))
                .perTaskRetryDelay(Duration.ofSeconds(5))
                .build();
        config = new CommonsOutboxAutoConfiguration();
    }

    @When("a persistor is created")
    public void persistorIsCreated() {
        persistor = config.persistor(properties, objectMapper, mock(Environment.class));
    }

    @Then("the persistor should be properly configured")
    public void thePersistorShouldBeProperlyConfigured() {
        assertThat(persistor)
                .as("persistor")
                .isNotNull()
                .isInstanceOf(DefaultPersistor.class);

        verify(objectMapper).registerModule(moduleCaptor.capture());

        assertThat(moduleCaptor.getValue())
                .as("registeredModule")
                .isInstanceOf(TransactionOutboxJacksonModule.class);
    }

    @Then("the persistor should use Jackson serialization")
    public void thePersistorShouldUseJacksonSerialization() {
        assertThat(persistor)
                .as("persistor")
                .isNotNull();

        // This is a bit tricky to test directly since the serializer is encapsulated.
        // We could use reflection to check, but that's not ideal.
        // Instead, we'll verify that the module was registered.
        verify(objectMapper).registerModule(any(TransactionOutboxJacksonModule.class));
    }
}
