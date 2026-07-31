package ru.itmo.wmp.mail.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MailStatusTransitionTest {
    @Test
    void shouldAllowValidTransitions() {
        assertTrue(
            MailStatusTransition.canMove(
                MailProcessingStatus.RECEIVED,
                MailProcessingStatus.PROCESSING
            )
        );

        assertTrue(
            MailStatusTransition.canMove(
                MailProcessingStatus.PROCESSING,
                MailProcessingStatus.PROCESSED
            )
        );

        assertTrue(
            MailStatusTransition.canMove(
                MailProcessingStatus.PROCESSING,
                MailProcessingStatus.FAILED
            )
        );
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(
            MailStatusTransition.canMove(
                MailProcessingStatus.RECEIVED,
                MailProcessingStatus.PROCESSED
            )
        );

        assertFalse(
            MailStatusTransition.canMove(
                MailProcessingStatus.RECEIVED,
                MailProcessingStatus.FAILED
            )
        );

        assertFalse(
            MailStatusTransition.canMove(
                MailProcessingStatus.PROCESSED,
                MailProcessingStatus.PROCESSING
            )
        );

        assertFalse(
            MailStatusTransition.canMove(
                MailProcessingStatus.FAILED,
                MailProcessingStatus.RECEIVED
            )
        );
    }
}
