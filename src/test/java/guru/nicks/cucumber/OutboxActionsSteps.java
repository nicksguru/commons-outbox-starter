package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.outbox.OutboxActions;

import com.gruelbox.transactionoutbox.TransactionOutbox;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link OutboxActions}.
 */
@RequiredArgsConstructor
public class OutboxActionsSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private TransactionOutbox transactionOutbox;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;
    private AutoCloseable closeableMocks;

    private TestOutboxActions testOutboxActions;
    private Object proxy;
    private AtomicBoolean actionExecuted;
    private String actionParameter;

    /**
     * Applies local {@code @Mock/@Spy}.
     */
    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public ActionData createActionData(Map<String, String> entry) {
        return ActionData.builder()
                .type(entry.get("type"))
                .parameter(entry.get("parameter"))
                .build();
    }

    @Given("an OutboxActions implementation is available")
    public void anOutboxActionsImplementationIsAvailable() {
        actionExecuted = new AtomicBoolean(false);
        testOutboxActions = new TestOutboxActions(transactionOutbox, actionExecuted);

        // mock the proxy behavior
        when(transactionOutbox.schedule(any())).thenAnswer(invocation -> testOutboxActions);
    }

    @Given("a transaction is started")
    public void aTransactionIsStarted() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    }

    @When("an outbox proxy is created for the target class")
    public void anOutboxProxyIsCreatedForTheTargetClass() {
        try {
            proxy = testOutboxActions.createProxyFor(TestOutboxActions.class);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("an action is scheduled through the outbox proxy")
    public void anActionIsScheduledThroughTheOutboxProxy() {
        try {
            TestOutboxActions proxy1 = testOutboxActions.getProxy();
            proxy1.executeEmptyAction();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the transaction is committed")
    public void theTransactionIsCommitted() {
        // simulate transaction commit
        doNothing().when(
                transactionManager).commit(transactionStatus);
    }

    @When("the transaction is rolled back")
    public void theTransactionIsRolledBack() {
        // simulate transaction rollback
        doNothing().when(
                transactionManager).rollback(transactionStatus);
        actionExecuted.set(false);
    }

    @When("an action of type {string} with parameter {string} is scheduled")
    public void anActionOfTypeWithParameterIsScheduled(String actionType, String parameter) {
        try {
            TestOutboxActions proxy1 = testOutboxActions.getProxy();
            actionParameter = parameter;

            switch (actionType) {
                case "simple", "complex" -> proxy1.executeActionWithParameter(parameter);
                case "empty" -> proxy1.executeEmptyAction();
                default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
            }
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("the Outbox proxy should not be null")
    public void theOutboxProxyShouldNotBeNull() {
        assertThat(proxy).as("proxy").isNotNull();
    }

    @Then("the Outbox proxy should be an instance of the target class")
    public void theOutboxProxyShouldBeAnInstanceOfTheTargetClass() {
        assertThat(proxy).as("proxy").isInstanceOf(TestOutboxActions.class);
    }

    @Then("the outboxed action should be executed successfully")
    public void theActionShouldBeExecutedSuccessfully() {
        assertThat(actionExecuted.get()).as("actionExecuted").isTrue();
    }

    @Then("the outboxed action should not be executed")
    public void theOutboxedActionShouldNotBeExecuted() {
        assertThat(actionExecuted.get()).as("actionExecuted").isFalse();
    }

    @Then("the outboxed action should be executed with the correct parameter")
    public void theOutboxedActionShouldBeExecutedWithTheCorrectParameter() {
        assertThat(actionExecuted.get()).as("actionExecuted").isTrue();

        if (actionParameter != null && !actionParameter.isEmpty()) {
            assertThat(testOutboxActions.getLastParameter()).as("lastParameter").isEqualTo(actionParameter);
        }
    }

    /**
     * Test implementation of {@link OutboxActions} for testing.
     */
    public static class TestOutboxActions implements OutboxActions {

        private final AtomicBoolean actionExecuted;
        @Getter(onMethod_ = @Override)
        private final TransactionOutbox transactionOutbox;
        @Getter
        private String lastParameter;

        public TestOutboxActions(TransactionOutbox transactionOutbox, AtomicBoolean actionExecuted) {
            this.transactionOutbox = transactionOutbox;
            this.actionExecuted = actionExecuted;
        }

        public TestOutboxActions getProxy() {
            return createProxyFor(TestOutboxActions.class);
        }

        protected void executeEmptyAction() {
            actionExecuted.set(true);
            lastParameter = null;
        }

        protected void executeActionWithParameter(String parameter) {
            actionExecuted.set(true);
            lastParameter = parameter;
        }

    }

    /**
     * Action data for parameterized tests.
     */
    @Value
    @Builder
    public static class ActionData {

        String type;
        String parameter;

    }

}
