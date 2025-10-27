package guru.nicks.cucumber;

import guru.nicks.outbox.domain.TransactionOutboxProperties;
import guru.nicks.outbox.domain.TransactionOutboxTaskBlockedEvent;
import guru.nicks.outbox.listener.TransactionOutboxTaskBlockedListener;

import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.TransactionOutboxEntry;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link TransactionOutboxTaskBlockedListener}.
 */
public class TransactionOutboxTaskUnblockerListenerSteps {

    @Mock
    private TransactionOutbox transactionOutbox;
    @Mock
    private TransactionOutboxEntry transactionOutboxEntry;
    private AutoCloseable closeableMocks;

    private TransactionOutboxTaskBlockedListener listener;
    private String taskId;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("transaction outbox properties with unblock blocked tasks set to {booleanValue}")
    public void transactionOutboxPropertiesWithUnblockBlockedTasksSetTo(boolean unblockBlockedTasks) {
        var properties = TransactionOutboxProperties.builder()
                .useJackson(false)
                .unblockBlockedTasks(unblockBlockedTasks)
                .blockAfterAttempts(3)
                .backgroundJobInitialDelay(Duration.ofSeconds(1))
                .backgroundJobRestartDelay(Duration.ofSeconds(1))
                .perTaskRetryDelay(Duration.ofSeconds(1))
                .build();

        taskId = UUID.randomUUID().toString();
        when(transactionOutboxEntry.getId())
                .thenReturn(taskId);

        listener = new TransactionOutboxTaskBlockedListener(properties, transactionOutbox);
    }

    @When("a transaction outbox task blocked event is received")
    public void aTransactionOutboxTaskBlockedEventIsReceived() {
        var event = new TransactionOutboxTaskBlockedEvent(transactionOutboxEntry);
        listener.onApplicationEvent(event);
    }

    @Then("the task should be unblocked")
    public void theTaskShouldBeUnblocked() {
        verify(transactionOutbox).unblock(taskId);
    }

    @Then("the task should remain blocked")
    public void theTaskShouldRemainBlocked() {
        verify(transactionOutbox, never()).unblock(taskId);
    }

}
