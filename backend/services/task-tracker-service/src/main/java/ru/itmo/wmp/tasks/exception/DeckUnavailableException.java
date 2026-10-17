package ru.itmo.wmp.tasks.exception;

public class DeckUnavailableException extends RuntimeException {
    public DeckUnavailableException(String action, Throwable cause) {
        super("Deck call failed: " + action, cause);
    }

    public DeckUnavailableException(String action) {
        super("Deck call failed: " + action);
    }
}
