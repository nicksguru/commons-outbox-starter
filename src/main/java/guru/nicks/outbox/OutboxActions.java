package guru.nicks.outbox;

import guru.nicks.outbox.config.TransactionOutboxAutoConfiguration;

import com.gruelbox.transactionoutbox.TransactionOutbox;

/**
 * Stores actions in {@link TransactionOutbox} and performs them. All public methods must be transactional (Mongo
 * transactions aren't supported by the library) - they join the caller's transaction, for example, to send a message
 * upon commit.
 * <p>
 * It's also possible to use Outbox without a parent transaction, just to retry some code: failed method names and
 * arguments are stored in DB and re-executed by a background job (but see caveats described in
 * {@link TransactionOutboxBackgroundJob it}).
 * <p>
 * Internal methods are {@code protected} because when decorated with {@link TransactionOutbox#schedule(Class)},
 * dependency injection doesn't work for {@code private} methods.
 */
public interface OutboxActions {

    TransactionOutbox getTransactionOutbox();

    /**
     * Calls {@link TransactionOutbox#initialize()} and {@link TransactionOutbox#schedule(Class)}. For the explanation
     * of why the first call is needed, see {@link TransactionOutboxAutoConfiguration}.
     *
     * @param targetClass usually {@link Object#getClass()} called from within a Spring bean
     * @param <T>         target class type
     * @return proxy for the target class
     */
    default <T> T createProxyFor(Class<T> targetClass) {
        return getTransactionOutbox().schedule(targetClass);
    }

}
