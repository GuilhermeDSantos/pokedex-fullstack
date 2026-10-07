package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.exception.EmailAlreadyRegisteredException;
import dev.guilhermeds.backend.domain.exception.UserAccountDataUnavailableException;
import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import dev.guilhermeds.backend.infrastructure.persistence.DatabaseFailures;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.UserAccountEntityMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {

    private static final String UNIQUE_EMAIL_CONSTRAINT = "uk_user_accounts_email";

    private final UserAccountJpaRepository jpaRepository;
    private final UserAccountEntityMapper mapper;

    public JpaUserAccountRepository(UserAccountJpaRepository jpaRepository, UserAccountEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UserAccount save(UserAccount account) {
        try {
            return reachable(() -> mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(account))));
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, UNIQUE_EMAIL_CONSTRAINT)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return reachable(() -> jpaRepository.findById(id.value()).map(mapper::toDomain));
    }

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        return reachable(() -> jpaRepository.findByEmail(email.value()).map(mapper::toDomain));
    }

    private static <T> T reachable(Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException exception) {
            if (DatabaseFailures.isUnreachable(exception)) {
                throw new UserAccountDataUnavailableException(exception);
            }
            throw exception;
        }
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        var cause = exception.getMostSpecificCause().getMessage();
        return cause != null && cause.contains(constraint);
    }
}
