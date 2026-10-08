package com.example.tasks.infrastructure.transaction;

import com.example.tasks.application.port.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

// The only transaction boundary; a real call rather than a proxy, so it works from anywhere.
@Component
public class SpringUnitOfWork implements UnitOfWork {

    private final TransactionTemplate template;

    public SpringUnitOfWork(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
