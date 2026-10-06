package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.RawPassword;

public record Credentials(Email email, RawPassword password) {
}
