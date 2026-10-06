package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.exception.EmailAlreadyRegisteredException;
import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.UserAccountEntityMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(account)));
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, UNIQUE_EMAIL_CONSTRAINT)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        var cause = exception.getMostSpecificCause().getMessage();
        return cause != null && cause.contains(constraint);
    }
}
