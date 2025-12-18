package guru.nicks.commons.outbox.domain;

import guru.nicks.commons.outbox.listener.TransactionOutboxTaskBlockedListener;

import com.gruelbox.transactionoutbox.Dialect;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "transaction-outbox")
@Validated
// immutability
@Value
@NonFinal // needed for CGLIB to bind property values (nested classes don't need this)
@Jacksonized
@Builder(toBuilder = true)
public class TransactionOutboxProperties {

    /**
     * {@link Dialect} (not an enumeration): MY_SQL_5, MY_SQL_8, POSTGRESQL_9, H2, ORACLE, MS_SQL_SERVER.
     */
    @NotBlank
    String dialect;

    /**
     * For flexible object serialization (any argument types in proxied methods, with polymorphism etc.) - see <a
     * href="https://github.com/gruelbox/transaction-outbox/blob/better-spring-example/README.md#flexible-serialization-beta"
     * >docs</a>.
     */
    boolean useJackson;

    /**
     * If true, {@link TransactionOutboxTaskBlockedListener} is called once a task has been blocked, thus retries last
     * forever.
     */
    boolean unblockBlockedTasks;

    /**
     * Max. number of attempts per task.
     *
     * @see #isUnblockBlockedTasks()
     */
    @Min(1)
    @NotNull
    Integer blockAfterAttempts;

    /**
     * Delay between the first background job invocation ({@link Scheduled#initialDelayString()}).
     */
    @NotNull
    Duration backgroundJobInitialDelay;

    /**
     * Delay between periodic background job invocations ({@link Scheduled#fixedDelayString()}). No matter how long the
     * job takes (it retries all tasks whose {@link #getPerTaskRetryDelay()} has already expired), the next invocation
     * will be delayed by the specified amount of time.
     */
    @NotNull
    Duration backgroundJobRestartDelay;

    /**
     * Per-task interval before it can be retried - checked by the background job on each run for each task.
     */
    @NotNull
    Duration perTaskRetryDelay;

}
