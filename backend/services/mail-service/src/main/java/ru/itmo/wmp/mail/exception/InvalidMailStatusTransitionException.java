package ru.itmo.wmp.mail.exception;

import ru.itmo.wmp.mail.domain.MailProcessingStatus;

public class InvalidMailStatusTransitionException extends RuntimeException {
    public InvalidMailStatusTransitionException(MailProcessingStatus from, MailProcessingStatus to) {
        super("Cannot change mail status from " + from + " to " + to);
    }
}
