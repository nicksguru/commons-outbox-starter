package guru.nicks.commons.outbox.config;

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
import guru.nicks.commons.outbox.TransactionOutboxBackgroundJob;
import guru.nicks.commons.outbox.domain.TransactionOutboxProperties;
import guru.nicks.commons.outbox.domain.TransactionOutboxTaskBlockedEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;

/**
 * Configures {@link TransactionOutbox} and its {@link Persistor}. This works with JPA transactions only (not with
 * MongoDB transactions).
 * <p>
 * WARNING: that bean is the same in each app, and the background job ({@link TransactionOutboxBackgroundJob}) is the
 * same too. See crucial comments in {@link TransactionOutboxBackgroundJob}.
 */
@AutoConfiguration
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
     * Creates {@link Persistor} bean if it's not already present. The dialect defaults to {@link Dialect#POSTGRESQL_9}
     * but can be overridden via the {@code transaction-outbox.dialect} property.
     */
    @ConditionalOnMissingBean(Persistor.class)
    @Bean
    public Persistor persistor(TransactionOutboxProperties properties,
            Converter<String, Dialect> outboxDialectConverter, ObjectMapper objectMapper, Environment environment) {
        // can't print ALL properties - they may contain sensitive data
        log.debug("Building {} bean using SQL dialect {}", Persistor.class.getSimpleName(), properties.getDialect());

        var builder = DefaultPersistor
                .builder()
                .dialect(outboxDialectConverter.convert(properties.getDialect()));

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

    /**
     * Converts a string to a {@link Dialect}. The latter is not an enumeration, therefore we need to check each value
     * manually and revamp the logic in case something changes in {@link Dialect}.
     * <p>
     * If the string does not match any known dialect, an {@link IllegalArgumentException} is thrown.
     */
    @Bean
    public Converter<String, Dialect> outboxDialectConverter() {
        // cannot be a Lambda, or Spring will fail with 'Unable to determine source type <S> and target type <T>
        // for your Converter'
        return new Converter<>() {
            @Override
            public @Nullable Dialect convert(String source) {
                return switch (source) {
                    case "MY_SQL_5" -> Dialect.MY_SQL_5;
                    case "MY_SQL_8" -> Dialect.MY_SQL_8;
                    case "POSTGRESQL_9" -> Dialect.POSTGRESQL_9;
                    case "H2" -> Dialect.H2;
                    case "ORACLE" -> Dialect.ORACLE;
                    case "MS_SQL_SERVER" -> Dialect.MS_SQL_SERVER;
                    default -> throw new IllegalArgumentException("Unknown dialect: '" + source + "'");
                };
            }
        };

    }

}
