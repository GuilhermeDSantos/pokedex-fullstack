package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.RegisterUserInput;
import dev.guilhermeds.backend.application.dto.UserOutput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;

import java.time.Instant;

public class RegisterUserInteractor implements RegisterUserUseCase {

    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final UserAccountMapper mapper;
    private final UnitOfWork unitOfWork;

    public RegisterUserInteractor(UserAccountRepository repository, PasswordHasher passwordHasher,
                                  UserAccountMapper mapper, UnitOfWork unitOfWork) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public UserOutput execute(RegisterUserInput input, UserId id, Instant now) {
        var registration = mapper.toRegistration(input);
        // BCrypt is deliberately slow, so it runs before the transaction holds a connection.
        var hash = passwordHasher.hash(registration.password());

        return unitOfWork.inTransaction(() -> {
            var account = mapper.toDomain(registration, hash, id, now);
            return UserOutput.from(repository.save(account));
        });
    }
}
