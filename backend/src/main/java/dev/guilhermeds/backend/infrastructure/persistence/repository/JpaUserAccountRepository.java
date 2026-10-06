package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.UserAccountEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {

    private final UserAccountJpaRepository jpaRepository;
    private final UserAccountEntityMapper mapper;

    public JpaUserAccountRepository(UserAccountJpaRepository jpaRepository, UserAccountEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UserAccount save(UserAccount account) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(account)));
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }
}
