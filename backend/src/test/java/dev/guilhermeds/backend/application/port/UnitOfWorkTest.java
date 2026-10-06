package dev.guilhermeds.backend.application.port;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class UnitOfWorkTest {

    private final List<String> events = new ArrayList<>();

    private final UnitOfWork unitOfWork = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            events.add("begin");
            var result = work.get();
            events.add("commit");
            return result;
        }
    };

    @Test
    void shouldRunWorkWithoutResultInsideTheTransaction() {
        unitOfWork.inTransaction(() -> {
            events.add("work");
        });

        assertThat(events).containsExactly("begin", "work", "commit");
    }
}
