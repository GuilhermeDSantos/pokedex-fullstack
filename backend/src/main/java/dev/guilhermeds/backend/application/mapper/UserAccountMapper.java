package dev.guilhermeds.backend.application.mapper;

import dev.guilhermeds.backend.application.dto.RegisterUserInput;
import dev.guilhermeds.backend.application.dto.Registration;
import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.FullName;
import dev.guilhermeds.backend.domain.model.PasswordHash;
import dev.guilhermeds.backend.domain.model.RawPassword;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;

import java.time.Instant;

public class UserAccountMapper {

    public Registration toRegistration(RegisterUserInput input) {
        return new Registration(new Email(input.email()), new FullName(input.name()), new RawPassword(input.password()));
    }

    public UserAccount toDomain(Registration registration, PasswordHash hash, UserId id, Instant now) {
        return UserAccount.register(id, registration.email(), registration.name(), hash, now);
    }
}
