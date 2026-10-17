package ru.itmo.wmp.tasks.infrastructure.deck;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.itmo.wmp.tasks.application.deck.DeckCardContent;
import ru.itmo.wmp.tasks.application.deck.DeckCardView;
import ru.itmo.wmp.tasks.application.deck.DeckGateway;
import ru.itmo.wmp.tasks.config.properties.DeckProperties;
import ru.itmo.wmp.tasks.exception.DeckUnavailableException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "deck", name = "enabled", havingValue = "true")
public class DeckRestClient implements DeckGateway {
    private static final String API_PATH = "/index.php/apps/deck/api/v1.0";
    private static final String OCS_API_REQUEST_HEADER = "OCS-APIRequest";
    private static final String CARD_TYPE = "plain";
    private static final int GROUP_PARTICIPANT = 1;
    private static final String BOARD_COLOR = "0082c9";

    private final RestClient restClient;
    private final String adminUser;

    public DeckRestClient(DeckProperties deckProperties, RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(deckProperties.timeout());
        requestFactory.setReadTimeout(deckProperties.timeout());

        this.adminUser = deckProperties.adminUser();
        this.restClient = restClientBuilder
            .baseUrl(deckProperties.baseUrl() + API_PATH)
            .requestFactory(requestFactory)
            .defaultHeader(OCS_API_REQUEST_HEADER, "true")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeaders(headers -> headers.setBasicAuth(
                deckProperties.adminUser(),
                deckProperties.adminPassword()
            ))
            .build();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Long createBoard(String title) {
        Map<String, Object> board = call(
            "create board " + title,
            () -> restClient.post()
                .uri("/boards")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", title, "color", BOARD_COLOR))
                .retrieve()
                .body(Map.class)
        );

        return number(board.get("id"));
    }

    @Override
    public Long createStack(Long boardId, String title, int order) {
        Map<String, Object> stack = call(
            "create stack " + title,
            () -> restClient.post()
                .uri("/boards/{boardId}/stacks", boardId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", title, "order", order))
                .retrieve()
                .body(Map.class)
        );

        return number(stack.get("id"));
    }

    @Override
    public DeckCardView createCard(Long boardId, Long stackId, DeckCardContent content) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("title", content.title());
        payload.put("description", content.description() == null ? "" : content.description());

        if (content.dueDate() != null) {
            payload.put("duedate", deckDate(content.dueDate()));
        }

        Map<String, Object> card = call(
            "create card " + content.title(),
            () -> restClient.post()
                .uri("/boards/{boardId}/stacks/{stackId}/cards", boardId, stackId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class)
        );

        return toView(card);
    }

    @Override
    public DeckCardView updateCard(
        Long boardId,
        Long stackId,
        Long cardId,
        DeckCardContent content
    ) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("title", content.title());
        payload.put("description", content.description() == null ? "" : content.description());
        payload.put("type", CARD_TYPE);
        payload.put("owner", adminUser);
        payload.put("duedate", content.dueDate() == null ? null : deckDate(content.dueDate()));

        Map<String, Object> card = call(
            "update card " + cardId,
            () -> restClient.put()
                .uri("/boards/{boardId}/stacks/{stackId}/cards/{cardId}", boardId, stackId, cardId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class)
        );

        return toView(card);
    }

    @Override
    public DeckCardView moveCard(Long boardId, Long stackId, Long cardId, Long targetStackId) {
        Map<String, Object> card = call(
            "move card " + cardId,
            () -> restClient.put()
                .uri(
                    "/boards/{boardId}/stacks/{stackId}/cards/{cardId}/reorder",
                    boardId,
                    stackId,
                    cardId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("order", 0, "stackId", targetStackId))
                .retrieve()
                .body(Map.class)
        );

        return card == null ? null : toView(card);
    }

    @Override
    public void assignUser(Long boardId, Long stackId, Long cardId, String nextcloudUid) {
        call(
            "assign " + nextcloudUid + " to card " + cardId,
            () -> restClient.put()
                .uri(
                    "/boards/{boardId}/stacks/{stackId}/cards/{cardId}/assignUser",
                    boardId,
                    stackId,
                    cardId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userId", nextcloudUid))
                .retrieve()
                .body(Map.class)
        );
    }

    @Override
    public void unassignUser(Long boardId, Long stackId, Long cardId, String nextcloudUid) {
        call(
            "unassign " + nextcloudUid + " from card " + cardId,
            () -> restClient.put()
                .uri(
                    "/boards/{boardId}/stacks/{stackId}/cards/{cardId}/unassignUser",
                    boardId,
                    stackId,
                    cardId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userId", nextcloudUid))
                .retrieve()
                .body(Map.class)
        );
    }

    @Override
    public List<DeckCardView> fetchCards(Long boardId) {
        List<Map<String, Object>> stacks = call(
            "fetch board " + boardId,
            () -> restClient.get()
                .uri("/boards/{boardId}/stacks", boardId)
                .retrieve()
                .body(List.class)
        );

        List<DeckCardView> cards = new ArrayList<>();

        if (stacks == null) {
            return cards;
        }

        for (Map<String, Object> stack : stacks) {
            Object rawCards = stack.get("cards");

            if (!(rawCards instanceof List<?> stackCards)) {
                continue;
            }

            for (Object rawCard : stackCards) {
                if (rawCard instanceof Map<?, ?> card) {
                    cards.add(toView(castToMap(card)));
                }
            }
        }

        return cards;
    }

    @Override
    public void shareBoardWithGroup(Long boardId, String groupId) {
        call(
            "share board " + boardId + " with group " + groupId,
            () -> restClient.post()
                .uri("/boards/{boardId}/acl", boardId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "type", GROUP_PARTICIPANT,
                    "participant", groupId,
                    "permissionEdit", true,
                    "permissionShare", false,
                    "permissionManage", false
                ))
                .retrieve()
                .body(Map.class)
        );
    }

    private <T> T call(String action, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException exception) {
            throw new DeckUnavailableException(action, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }

    private DeckCardView toView(Map<String, Object> card) {
        List<String> assigned = new ArrayList<>();

        if (card.get("assignedUsers") instanceof List<?> users) {
            for (Object user : users) {
                if (user instanceof Map<?, ?> entry
                    && entry.get("participant") instanceof Map<?, ?> participant) {
                    assigned.add(String.valueOf(participant.get("uid")));
                }
            }
        }

        return new DeckCardView(
            number(card.get("id")),
            number(card.get("stackId")),
            string(card.get("title")),
            string(card.get("description")),
            localDate(card.get("duedate")),
            assigned,
            number(card.get("lastModified")),
            card.get("deletedAt") != null && number(card.get("deletedAt")) != null
                && number(card.get("deletedAt")) > 0
        );
    }

    private String deckDate(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private LocalDate localDate(Object raw) {
        if (raw == null) {
            return null;
        }

        String value = String.valueOf(raw);

        return value.length() < 10 ? null : LocalDate.parse(value.substring(0, 10));
    }

    private String string(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private Long number(Object raw) {
        if (raw instanceof Number value) {
            return value.longValue();
        }

        return null;
    }
}
