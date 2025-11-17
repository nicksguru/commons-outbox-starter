package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.outbox.TransactionOutboxBackgroundJob;

import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.TransactionOutboxEntry;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

/**
 * Step definitions for testing {@link TransactionOutboxBackgroundJob}.
 */
@RequiredArgsConstructor
public class TransactionOutboxBackgroundJobSteps {

    // DI
    private final TextWorld textWorld;

    private final List<Boolean> flushResults = new ArrayList<>();
    private TransactionOutboxBackgroundJob backgroundJob;
    private Duration initialDelay;
    private Duration restartDelay;

    private TransactionOutbox transactionOutbox;
    private AtomicInteger flushCallCount;

    @Given("the transaction outbox background job is initialized")
    public void theTransactionOutboxBackgroundJobIsInitialized() {
        // default initialization
        initialDelay = Duration.ofMinutes(5);
        restartDelay = Duration.ofMinutes(10);
    }

    @Given("the initial delay is set to {string}")
    public void theInitialDelayIsSetTo(String delay) {
        initialDelay = parseDuration(delay);
    }

    @Given("the restart delay is set to {string}")
    public void theRestartDelayIsSetTo(String delay) {
        restartDelay = parseDuration(delay);
    }

    @When("the background job is set up")
    public void theBackgroundJobIsSetUp() {
        // set the fields using reflection
        try {
            backgroundJob = new TransactionOutboxBackgroundJob(transactionOutbox);

            Field initialDelayField = backgroundJob.getClass().getDeclaredField("initialDelay");
            initialDelayField.setAccessible(true);
            initialDelayField.set(backgroundJob, initialDelay);

            Field restartDelayField = backgroundJob.getClass().getDeclaredField("restartDelay");
            restartDelayField.setAccessible(true);
            restartDelayField.set(backgroundJob, restartDelay);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set fields via reflection", e);
        }
    }

    @When("the background job retries failed tasks")
    public void theBackgroundJobRetriesFailedTasks() {
        try {
            backgroundJob.retryFailedTasks();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("there are tasks to process")
    public void thereAreTasksToProcess() {
        // setup outbox to return true once then false (indicating tasks were processed)
        flushResults.add(true);
        flushResults.add(false);
        setupOutboxFlushBehavior();
    }

    @When("the transaction outbox throws an exception")
    public void theTransactionOutboxThrowsAnException() {
        doThrow(new RuntimeException("Test exception"))
                .when(transactionOutbox).flush();

        try {
            backgroundJob.retryFailedTasks();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("all tasks should be processed")
    public void allTasksShouldBeProcessed() {
        // verify flush was called the expected number of times
        assertThat(flushCallCount.get()).isEqualTo(2);
    }

    /**
     * Sets up the behavior of the outbox.flush() method based on flushResults.
     */
    private void setupOutboxFlushBehavior() {
        flushCallCount = new AtomicInteger();

        transactionOutbox = new TransactionOutbox() {
            @Override
            public void initialize() {
                // do nothing
            }

            @Override
            public <T> T schedule(Class<T> clazz) {
                return null;
            }

            @Override
            public ParameterizedScheduleBuilder with() {
                return null;
            }

            @Override
            public boolean flush(Executor executor) {
                boolean result = flushResults.isEmpty()
                        ? false
                        : flushResults.get(flushCallCount.getAndIncrement() % flushResults.size());
                return result;
            }

            @Override
            public boolean flushTopics(Executor executor, List<String> topicNames) {
                return false;
            }

            @Override
            public boolean unblock(String entryId) {
                return false;
            }

            @Override
            public boolean unblock(String entryId, Object transactionContext) {
                return false;
            }

            @Override
            public void processNow(TransactionOutboxEntry entry) {
                // do nothing
            }
        };
    }

    /**
     * Parses a duration string like "5 minutes" into a Duration object.
     *
     * @param durationStr the duration string
     * @return the Duration object
     */
    private Duration parseDuration(String durationStr) {
        if (durationStr.contains("minute")) {
            return Duration.ofMinutes(Long.parseLong(durationStr.replaceAll("[^0-9]", "")));
        }

        if (durationStr.contains("second")) {
            return Duration.ofSeconds(Long.parseLong(durationStr.replaceAll("[^0-9]", "")));
        }

        if (durationStr.contains("hour")) {
            return Duration.ofHours(Long.parseLong(durationStr.replaceAll("[^0-9]", "")));
        }

        return Duration.ofMinutes(5); // default
    }

    @When("the transaction outbox is set up to throw an exception")
    public void theTransactionOutboxIsSetUpToThrowAnException() {
        flushCallCount = new AtomicInteger();

        transactionOutbox = new TransactionOutbox() {
            @Override
            public void initialize() {
                // do nothing
            }

            @Override
            public <T> T schedule(Class<T> clazz) {
                return null;
            }

            @Override
            public ParameterizedScheduleBuilder with() {
                return null;
            }

            @Override
            public boolean flush(Executor executor) {
                throw new RuntimeException("Test exception from the 'flush' method");
            }

            @Override
            public boolean flushTopics(Executor executor, List<String> topicNames) {
                return false;
            }

            @Override
            public boolean unblock(String entryId) {
                return false;
            }

            @Override
            public boolean unblock(String entryId, Object transactionContext) {
                return false;
            }

            @Override
            public void processNow(TransactionOutboxEntry entry) {
                // do nothing
            }
        };
    }

}
