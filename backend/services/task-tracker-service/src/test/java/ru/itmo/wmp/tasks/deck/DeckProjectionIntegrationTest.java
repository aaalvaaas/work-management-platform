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
import ru.itmo.wmp.tasks.application.deck.DeckCardContent;
import ru.itmo.wmp.tasks.application.deck.DeckCardView;
import ru.itmo.wmp.tasks.application.deck.DeckGateway;
import ru.itmo.wmp.tasks.application.deck.DeckProjectionService;
import ru.itmo.wmp.tasks.application.deck.DeckPushWorker;
import ru.itmo.wmp.tasks.application.deck.repository.DeckCardRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.deck.DeckCard;
import ru.itmo.wmp.tasks.domain.deck.DeckSyncStatus;
import ru.itmo.wmp.tasks.exception.DeckUnavailableException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@IntegrationTest
public class DeckProjectionIntegrationTest {
    private static final Long ORGANIZATION = 10L;
    private static final Long BLOCK = 3L;
    private static final Long MANAGER = 100L;
    private static final Long BOARD = 777L;
    private static final Long NEW_STACK = 71L;
    private static final Long PROGRESS_STACK = 72L;
    private static final Long CARD = 500L;

    @Autowired
    private TaskService taskService;

    @Autowired
    private DeckPushWorker deckPushWorker;

    @Autowired
    private DeckProjectionService deckProjectionService;

    @Autowired
    private DeckCardRepository deckCardRepository;

    @MockitoBean
    private PlatformGateway platformGateway;

    @MockitoBean
    private DeckGateway deckGateway;

    @BeforeEach
    void stubDependencies() {
        when(platformGateway.membershipOf(any())).thenReturn(new OrganizationMembership(
            ORGANIZATION,
            MANAGER,
            false,
            true,
            Set.of("MANAGER"),
            List.of()
        ));

        when(platformGateway.organizationOf(any()))
            .thenReturn(new OrganizationSummary(ORGANIZATION, "megafaculty", "Мегафакультет"));

        when(platformGateway.blockOf(any(), any()))
            .thenReturn(new BlockSummary(BLOCK, ORGANIZATION, "Разработка"));

        when(platformGateway.requireActiveParticipant(anyLong(), anyString()))
            .thenReturn(new ParticipantSummary("465824", 300L, "Иванов Иван"));

        when(deckGateway.isEnabled()).thenReturn(true);
        when(deckGateway.createBoard(anyString())).thenReturn(BOARD);
        when(deckGateway.createStack(any(), eq("Новые"), anyInt())).thenReturn(NEW_STACK);
        when(deckGateway.createStack(any(), eq("В работе"), anyInt())).thenReturn(PROGRESS_STACK);
        when(deckGateway.createStack(any(), anyString(), anyInt())).thenReturn(99L);
        when(deckGateway.createStack(any(), eq("Новые"), anyInt())).thenReturn(NEW_STACK);
        when(deckGateway.createStack(any(), eq("В работе"), anyInt())).thenReturn(PROGRESS_STACK);
        when(deckGateway.createCard(any(), any(), any()))
            .thenReturn(new DeckCardView(CARD, NEW_STACK, "Задача", null, null, List.of(), 1000L, false));
        when(deckGateway.updateCard(any(), any(), any(), any()))
            .thenReturn(new DeckCardView(CARD, PROGRESS_STACK, "Задача", null, null, List.of(), 2000L, false));
    }

    @Test
    void shouldMarkNewTaskAsPendingAndPushItToDeck() {
        Task task = createTask(false);

        DeckCard waiting = deckCardRepository.findByTaskId(task.getId()).orElseThrow();

        assertThat(waiting.getSyncStatus()).isEqualTo(DeckSyncStatus.PENDING);
        assertThat(waiting.getDeckCardId()).isNull();

        deckPushWorker.push();

        DeckCard pushed = deckCardRepository.findByTaskId(task.getId()).orElseThrow();

        assertThat(pushed.getSyncStatus()).isEqualTo(DeckSyncStatus.SYNCED);
        assertThat(pushed.getDeckCardId()).isEqualTo(CARD);
        assertThat(pushed.getDeckBoardId()).isEqualTo(BOARD);
        assertThat(pushed.getAssignedUid()).isEqualTo("465824");

        verify(deckGateway).createCard(eq(BOARD), eq(NEW_STACK), any(DeckCardContent.class));
        verify(deckGateway).assignUser(BOARD, NEW_STACK, CARD, "465824");
    }

    @Test
    void shouldMoveCardWhenStatusChanges() {
        Task task = createTask(false);

        deckPushWorker.push();

        taskService.changeStatus(task.getId(), TaskStatus.IN_PROGRESS, MANAGER);

        assertThat(deckCardRepository.findByTaskId(task.getId()).orElseThrow().getSyncStatus())
            .isEqualTo(DeckSyncStatus.PENDING);

        deckPushWorker.push();

        verify(deckGateway).moveCard(BOARD, NEW_STACK, CARD, PROGRESS_STACK);

        assertThat(deckCardRepository.findByTaskId(task.getId()).orElseThrow().getDeckStackId())
            .isEqualTo(PROGRESS_STACK);
    }

    @Test
    void shouldKeepTaskWorkingWhenDeckFails() {
        when(deckGateway.createCard(any(), any(), any()))
            .thenThrow(new DeckUnavailableException("create card"));

        Task task = createTask(false);

        deckPushWorker.push();

        DeckCard failed = deckCardRepository.findByTaskId(task.getId()).orElseThrow();

        assertThat(failed.getSyncStatus()).isEqualTo(DeckSyncStatus.FAILED);
        assertThat(failed.getLastError()).contains("create card");
    }

    @Test
    void shouldRetryFailedProjectionOnRequest() {
        when(deckGateway.createCard(any(), any(), any()))
            .thenThrow(new DeckUnavailableException("create card"));

        Task task = createTask(false);

        deckPushWorker.push();

        doReturn(new DeckCardView(CARD, NEW_STACK, "Задача", null, null, List.of(), 1000L, false))
            .when(deckGateway)
            .createCard(any(), any(), any());

        taskService.requestDeckSync(task.getId(), MANAGER);
        deckPushWorker.push();

        assertThat(deckCardRepository.findByTaskId(task.getId()).orElseThrow().getSyncStatus())
            .isEqualTo(DeckSyncStatus.SYNCED);
    }

    @Test
    void shouldDoNothingWhenIntegrationIsDisabled() {
        when(deckGateway.isEnabled()).thenReturn(false);

        Task task = createTask(false);

        assertThat(deckProjectionService.cardOf(task.getId())).isEmpty();

        deckPushWorker.push();

        verify(deckGateway, never()).createCard(any(), any(), any());
    }

    private Task createTask(boolean requiresApproval) {
        return taskService.create(
            new TaskCommand(
                ORGANIZATION,
                BLOCK,
                "Задача",
                "описание",
                "465824",
                null,
                requiresApproval
            ),
            MANAGER
        );
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
