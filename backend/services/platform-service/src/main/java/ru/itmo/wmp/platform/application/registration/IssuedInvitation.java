package ru.itmo.wmp.platform.application.registration;

import ru.itmo.wmp.platform.domain.registration.Invitation;

public record IssuedInvitation(
    Invitation invitation,
    String token
) {
}
