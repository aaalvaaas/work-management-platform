package ru.itmo.wmp.tasks.domain.deck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "deck_cards")
@Getter
@Setter
public class DeckCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column
    private Long deckBoardId;

    @Column
    private Long deckStackId;

    @Column
    private Long deckCardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckSyncStatus syncStatus;

    @Column(length = 500)
    private String lastError;

    @Column
    private Long deckLastModified;

    @Column
    private LocalDateTime lastSyncedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
