package guru.nicks.commons.outbox;

import guru.nicks.commons.utils.TimeUtils;

import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.spring.SpringInstantiator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Retries background {@link TransactionOutbox} tasks. Unfortunately it seems impossible to log individual task errors -
 * {@code TransactionOutboxImpl} logs them with Slf4J. The initial delay is in the
 * {@code transaction-outbox.backgroundJobInitialDelay} config setting, and the restart delay is in the
 * {@code transaction-outbox.backgroundJobRestartDelay} config setting.
 * <p>
 * This job in THE SAME for all apps that use Outbox. It processes ALL tasks, which means parallel processing and
 * increased throughput (there are no race conditions - tasks are locked before processing with
 * {@code SELECT FOR UPDATE SKIP LOCKED} in modern SQL engines / {@code SELECT FOR UPDATE} in older ones).
 * <p>
 * <b>Not all apps can execute all tasks</b> - the tasks refer to Spring beans (by their name - see
 * {@link SpringInstantiator} for details) which may not exist. Such useless retries simply increase the number of
 * task's failed attempts, which should be taken into account when configuring the max. number of attempts.
 * <p>
 * <b>WARNING:</b> if different apps have same-named beans with same-signature methods doing different things, strange
 * things may happen, depending on which of the apps picks up a particular task.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionOutboxBackgroundJob {

    // DI
    private final TransactionOutbox outbox;

    @Value("${transaction-outbox.backgroundJobInitialDelay}")
    private Duration initialDelay;
    @Value("${transaction-outbox.backgroundJobRestartDelay}")
    private Duration restartDelay;

    @PostConstruct
    private void init() {
        log.debug("TransactionOutbox background jobs will start in {} "
                        + "and restart (no matter how long the job takes) every {}",
                TimeUtils.humanFormatDuration(initialDelay),
                TimeUtils.humanFormatDuration(restartDelay));
    }

    @Scheduled(initialDelayString = "${transaction-outbox.backgroundJobInitialDelay}",
            fixedDelayString = "${transaction-outbox.backgroundJobRestartDelay}")
    public void retryFailedTasks() {
        try {
            do {
                log.trace("Processing TransactionOutbox tasks in background...");
            } while (outbox.flush());
        }
        // these are NOT errors inside outbox tasks, these are errors inside TransactionOutbox itself, which hardly
        // ever happen
        catch (Exception e) {
            log.error("Internal error flushing TransactionOutbox (will retry in {}): {}",
                    TimeUtils.humanFormatDuration(restartDelay), e.getMessage(), e);
        }
    }

}
