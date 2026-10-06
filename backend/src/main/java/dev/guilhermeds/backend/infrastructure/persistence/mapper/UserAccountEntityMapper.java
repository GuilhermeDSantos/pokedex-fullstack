package dev.guilhermeds.backend.infrastructure.persistence.mapper;

import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.FullName;
import dev.guilhermeds.backend.domain.model.PasswordHash;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.stereotype.Component;

@Component
public class UserAccountEntityMapper {

    public UserAccountEntity toEntity(UserAccount account) {
        var entity = new UserAccountEntity();
        entity.setId(account.getId().value());
        entity.setEmail(account.getEmail().value());
        entity.setName(account.getName().value());
        entity.setPasswordHash(account.getPasswordHash().value());
        entity.setCreatedAt(account.getCreatedAt());
        return entity;
    }

    public UserAccount toDomain(UserAccountEntity entity) {
        return UserAccount.builder()
            .id(new UserId(entity.getId()))
            .email(new Email(entity.getEmail()))
            .name(new FullName(entity.getName()))
            .passwordHash(new PasswordHash(entity.getPasswordHash()))
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
