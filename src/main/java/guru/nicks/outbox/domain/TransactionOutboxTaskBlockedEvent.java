package guru.nicks.outbox.domain;

import com.gruelbox.transactionoutbox.TransactionOutboxEntry;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a task is blocked due to too many retry failures.
 *
 * @see guru.nicks.outbox.listener.TransactionOutboxTaskBlockedListener
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TransactionOutboxTaskBlockedEvent extends ApplicationEvent {

    public TransactionOutboxTaskBlockedEvent(TransactionOutboxEntry source) {
        super(source);
    }

    /**
     * @return {@link #getSource()} cast to {@link TransactionOutboxEntry}
     */
    public TransactionOutboxEntry getTask() {
        return (TransactionOutboxEntry) getSource();
    }

}
