package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.outbox.config.CommonsOutboxAutoConfiguration;
import guru.nicks.commons.outbox.domain.TransactionOutboxProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gruelbox.transactionoutbox.Dialect;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.spring.SpringInstantiator;
import com.gruelbox.transactionoutbox.spring.SpringTransactionManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Map;

import static org.mockito.Mockito.mock;

@RequiredArgsConstructor
public class TransactionOutboxConfigSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private SpringTransactionManager outboxTransactionManager;
    @Mock
    private SpringInstantiator outboxInstantiator;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    private AutoCloseable closeableMocks;

    private TransactionOutboxProperties properties;
    private CommonsOutboxAutoConfiguration config;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public TransactionOutboxProperties createTransactionOutboxProperties(Map<String, String> entry) {
        return TransactionOutboxProperties.builder()
                .dialect("POSTGRESQL_9")
                .useJackson(Boolean.parseBoolean(entry.get("useJackson")))
                .unblockBlockedTasks(Boolean.parseBoolean(entry.get("unblockBlockedTasks")))
                .blockAfterAttempts(Integer.parseInt(entry.get("blockAfterAttempts")))
                .backgroundJobInitialDelay(Duration.parse(entry.get("backgroundJobInitialDelay")))
                .backgroundJobRestartDelay(Duration.parse(entry.get("backgroundJobRestartDelay")))
                .perTaskRetryDelay(Duration.parse(entry.get("perTaskRetryDelay")))
                .build();
    }

    @Given("transaction outbox properties are configured with:")
    public void transactionOutboxPropertiesAreConfiguredWith(TransactionOutboxProperties propertiesData) {
        try {
            properties = TransactionOutboxProperties.builder()
                    .dialect("POSTGRESQL_9")
                    .useJackson(propertiesData.isUseJackson())
                    .unblockBlockedTasks(propertiesData.isUnblockBlockedTasks())
                    .blockAfterAttempts(propertiesData.getBlockAfterAttempts())
                    .backgroundJobInitialDelay(propertiesData.getBackgroundJobInitialDelay())
                    .backgroundJobRestartDelay(propertiesData.getBackgroundJobRestartDelay())
                    .perTaskRetryDelay(propertiesData.getPerTaskRetryDelay())
                    .build();

            config = new CommonsOutboxAutoConfiguration();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("transaction outbox is created")
    public void theTransactionOutboxBeanIsCreated() {
        var persistor = config.persistor(properties, str -> Dialect.POSTGRESQL_9,
                new ObjectMapper(), mock(Environment.class));

        TransactionOutbox transactionOutbox = config.transactionOutbox(
                outboxTransactionManager,
                outboxInstantiator,
                persistor,
                applicationEventPublisher,
                properties);
    }

}
