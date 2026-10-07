package dev.guilhermeds.backend.infrastructure.transaction;

import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.exception.TransactionUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    public SpringUnitOfWork(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        try {
            return transactionTemplate.execute(status -> work.get());
        } catch (CannotCreateTransactionException exception) {
            throw new TransactionUnavailableException(exception);
        }
    }
}
