package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.FullName;
import dev.guilhermeds.backend.domain.model.RawPassword;

public record Registration(Email email, FullName name, RawPassword password) {
}
