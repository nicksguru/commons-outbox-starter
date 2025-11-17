package guru.nicks.commons.outbox.listener;

import guru.nicks.commons.outbox.domain.TransactionOutboxProperties;
import guru.nicks.commons.outbox.domain.TransactionOutboxTaskBlockedEvent;

import com.gruelbox.transactionoutbox.TransactionOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to {@link TransactionOutboxTaskBlockedEvent} and unblocks the task if
 * {@link TransactionOutboxProperties#isUnblockBlockedTasks()} is {@code true}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionOutboxTaskBlockedListener implements ApplicationListener<TransactionOutboxTaskBlockedEvent> {

    // DI
    private final TransactionOutboxProperties properties;
    private final TransactionOutbox transactionOutbox;

    @Transactional
    @Override
    public void onApplicationEvent(TransactionOutboxTaskBlockedEvent event) {
        if (!properties.isUnblockBlockedTasks()) {
            return;
        }

        log.warn("Task '{}' was blocked due to excessive number of failed attempts. "
                + "Unblocking and resetting the number of attempts to resume retries.", event.getTask().getId());
        transactionOutbox.unblock(event.getTask().getId());
    }

}
