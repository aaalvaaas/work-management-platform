package ru.itmo.wmp.tasks.deck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.itmo.wmp.tasks.IntegrationTest;
import ru.itmo.wmp.tasks.application.BlockSummary;
import ru.itmo.wmp.tasks.application.OrganizationMembership;
import ru.itmo.wmp.tasks.application.OrganizationSummary;
import ru.itmo.wmp.tasks.application.ParticipantSummary;
import ru.itmo.wmp.tasks.application.PlatformGateway;
import ru.itmo.wmp.tasks.application.TaskCommand;
import ru.itmo.wmp.tasks.application.TaskService;
import ru.itmo.wmp.tasks.application.deck.DeckCardView;
import ru.itmo.wmp.tasks.application.deck.DeckGateway;
import ru.itmo.wmp.tasks.application.deck.DeckPullWorker;
import ru.itmo.wmp.tasks.application.deck.DeckPushWorker;
import ru.itmo.wmp.tasks.application.deck.repository.DeckCardRepository;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@IntegrationTest
public class DeckPullIntegrationTest {
    private static final Long ORGANIZATION = 10L;
    private static final Long BLOCK = 3L;
    private static final Long MANAGER = 100L;
    private static final Long BOARD = 777L;
    private static final Long NEW_STACK = 71L;
    private static final Long PROGRESS_STACK = 72L;
    private static final Long DONE_STACK = 74L;
    private static final Long CARD = 500L;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DeckPushWorker deckPushWorker;

    @Autowired
    private DeckPullWorker deckPullWorker;

    @Autowired
    private DeckCardRepository deckCardRepository;

    @MockitoBean
    private PlatformGateway platformGateway;

    @MockitoBean
    private DeckGateway deckGateway;

    @BeforeEach
    void stubDependencies() {
        when(platformGateway.membershipOf(any())).thenReturn(new OrganizationMembership(
            ORGANIZATION, MANAGER, false, true, Set.of("MANAGER"), List.of()
        ));
        when(platformGateway.organizationOf(any()))
            .thenReturn(new OrganizationSummary(ORGANIZATION, "megafaculty", "Мегафакультет"));
        when(platformGateway.blockOf(any(), any()))
            .thenReturn(new BlockSummary(BLOCK, ORGANIZATION, "Разработка"));
        when(platformGateway.requireActiveParticipant(anyLong(), anyString()))
            .thenReturn(new ParticipantSummary("465824", 300L, "Иванов Иван"));

        when(deckGateway.isEnabled()).thenReturn(true);
        when(deckGateway.createBoard(anyString())).thenReturn(BOARD);
        when(deckGateway.createStack(any(), anyString(), anyInt())).thenReturn(99L);
        when(deckGateway.createStack(any(), eq("Новые"), anyInt())).thenReturn(NEW_STACK);
        when(deckGateway.createStack(any(), eq("В работе"), anyInt())).thenReturn(PROGRESS_STACK);
        when(deckGateway.createStack(any(), eq("Готово"), anyInt())).thenReturn(DONE_STACK);
        when(deckGateway.createCard(any(), any(), any()))
            .thenReturn(view(NEW_STACK, "Задача", 1000L));
        when(deckGateway.updateCard(any(), any(), any(), any()))
            .thenReturn(view(NEW_STACK, "Задача", 1000L));
    }

    @Test
    void shouldApplyEditsMadeInDeck() {
        Task task = projectedTask(false);

        doReturn(List.of(new DeckCardView(
            CARD,
            NEW_STACK,
            "Название из Deck",
            "описание из Deck",
            LocalDate.of(2026, 11, 20),
            List.of(),
            5000L,
            false
        ))).when(deckGateway).fetchCards(BOARD);

        deckPullWorker.pull();

        Task updated = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(updated.getTitle()).isEqualTo("Название из Deck");
        assertThat(updated.getDescription()).isEqualTo("описание из Deck");
        assertThat(updated.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void shouldMoveTaskToTheStatusOfItsColumn() {
        Task task = projectedTask(false);

        doReturn(List.of(view(PROGRESS_STACK, "Задача", 5000L)))
            .when(deckGateway).fetchCards(BOARD);

        deckPullWorker.pull();

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getStatus())
            .isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void shouldRefuseToCloseTaskThatRequiresApprovalFromDeck() {
        Task task = projectedTask(true);

        doReturn(List.of(view(DONE_STACK, "Задача", 5000L)))
            .when(deckGateway).fetchCards(BOARD);

        deckPullWorker.pull();

        Task unchanged = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(unchanged.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(deckCardRepository.findByTaskId(task.getId()).orElseThrow().getSyncStatus())
            .isEqualTo(DeckSyncStatus.PENDING);
    }

    @Test
    void shouldIgnoreCardThatDidNotChange() {
        Task task = projectedTask(false);

        doReturn(List.of(view(NEW_STACK, "Старое название", 1000L)))
            .when(deckGateway).fetchCards(BOARD);

        deckPullWorker.pull();

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getTitle())
            .isEqualTo("Задача");
    }

    private Task projectedTask(boolean requiresApproval) {
        Task task = taskService.create(
            new TaskCommand(ORGANIZATION, BLOCK, "Задача", "описание", "465824", null, requiresApproval),
            MANAGER
        );

        deckPushWorker.push();

        return task;
    }

    private DeckCardView view(Long stackId, String title, Long lastModified) {
        return new DeckCardView(CARD, stackId, title, "описание", null, List.of(), lastModified, false);
    }
}
