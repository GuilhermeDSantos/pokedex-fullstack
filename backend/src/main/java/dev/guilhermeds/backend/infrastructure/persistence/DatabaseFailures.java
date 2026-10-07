package dev.guilhermeds.backend.infrastructure.persistence;

import org.hibernate.TransactionException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

// One definition of "the database can't be reached", whatever layer of Spring/Hibernate wraps it.
public final class DatabaseFailures {

    private DatabaseFailures() {
    }

    public static boolean isUnreachable(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLTransientConnectionException || cause instanceof CannotCreateTransactionException) {
                return true;
            }
            // SQLState class 08 is a connection exception; 57P0x means the server shut down or crashed.
            if (cause instanceof SQLException sql && sql.getSQLState() != null
                && (sql.getSQLState().startsWith("08") || sql.getSQLState().startsWith("57P"))) {
                return true;
            }
            // A rollback or commit that can't reach the database replaces the query's own error, and the
            // pool's "Connection is closed" carries no SQLState: the failed transaction itself is the sign.
            if (cause instanceof TransactionException) {
                return true;
            }
        }
        return false;
    }
}
