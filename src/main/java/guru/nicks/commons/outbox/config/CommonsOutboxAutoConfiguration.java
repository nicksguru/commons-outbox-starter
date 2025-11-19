package guru.nicks.commons.outbox.config;

import guru.nicks.commons.outbox.TransactionOutboxBackgroundJob;
import guru.nicks.commons.outbox.domain.TransactionOutboxProperties;
import guru.nicks.commons.outbox.domain.TransactionOutboxTaskBlockedEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gruelbox.transactionoutbox.DefaultPersistor;
import com.gruelbox.transactionoutbox.Dialect;
import com.gruelbox.transactionoutbox.Persistor;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.gruelbox.transactionoutbox.TransactionOutboxEntry;
import com.gruelbox.transactionoutbox.TransactionOutboxListener;
import com.gruelbox.transactionoutbox.jackson.JacksonInvocationSerializer;
import com.gruelbox.transactionoutbox.jackson.TransactionOutboxJacksonModule;
import com.gruelbox.transactionoutbox.spring.SpringInstantiator;
import com.gruelbox.transactionoutbox.spring.SpringTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configures {@link TransactionOutbox} and its {@link Persistor}. This works with JPA transactions only (not with
 * MongoDB transactions).
 * <p>
 * WARNING: that bean is the same in each app, and the background job ({@link TransactionOutboxBackgroundJob}) is the
 * same too. See crucial comments in {@link TransactionOutboxBackgroundJob}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TransactionOutboxProperties.class)
@Import({SpringInstantiator.class, SpringTransactionManager.class})
@Slf4j
public class CommonsOutboxAutoConfiguration {

    /**
     * Creates {@link TransactionOutbox} bean if it's not already present.
     */
    @ConditionalOnMissingBean(TransactionOutbox.class)
    @Bean
    public TransactionOutbox transactionOutbox(
            // bean configures itself by grabbing DataSource bean
            SpringTransactionManager outboxTransactionManager,
            // doesn't call class constructor, rather retrieves beans from app context
            SpringInstantiator outboxInstantiator,
            Persistor outboxPersistor,
            ApplicationEventPublisher applicationEventPublisher,
            TransactionOutboxProperties properties) {
        log.debug("Building {} bean using properties: {}", TransactionOutbox.class.getSimpleName(), properties);

        return TransactionOutbox.builder()
                .transactionManager(outboxTransactionManager)
                .instantiator(outboxInstantiator)
                .persistor(outboxPersistor)
                .attemptFrequency(properties.getPerTaskRetryDelay())
                .blockAfterAttempts(properties.getBlockAfterAttempts())
                .listener(new TransactionOutboxListener() {
                    @Override
                    public void success(TransactionOutboxEntry task) {
                        log.info("Outboxed task succeeded: {}", task);
                    }

                    @Override
                    public void blocked(TransactionOutboxEntry task, Throwable cause) {
                        if (!properties.isUnblockBlockedTasks()) {
                            log.error("Outboxed task '{}' blocked after too many retry failures: {}",
                                    task.getId(), cause.toString());
                        }

                        applicationEventPublisher.publishEvent(new TransactionOutboxTaskBlockedEvent(task));
                    }
                })
                //.initializeImmediately(false)
                .build();
    }

    /**
     * Creates {@link Persistor} bean if it's not already present.
     */
    @ConditionalOnMissingBean(Persistor.class)
    @Bean
    public Persistor persistor(TransactionOutboxProperties properties, ObjectMapper objectMapper) {
        log.debug("Building {} bean using properties: {}", Persistor.class.getSimpleName(), properties);

        var builder = DefaultPersistor.builder()
                .dialect(Dialect.POSTGRESQL_9);

        if (properties.isUseJackson()) {
            builder.serializer(JacksonInvocationSerializer
                    .builder()
                    .mapper(objectMapper)
                    .build());
        }

        // add serializers for Invocation and TransactionOutboxEntry in case they need to be sent somewhere
        objectMapper.registerModule(new TransactionOutboxJacksonModule());
        return builder.build();
    }

}
