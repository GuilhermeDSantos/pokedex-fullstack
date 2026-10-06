package dev.guilhermeds.backend.domain.repository;

import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;

import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findById(UserId id);

    Optional<UserAccount> findByEmail(Email email);
}
