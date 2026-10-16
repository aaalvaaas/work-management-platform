package ru.itmo.wmp.platform.application.imports;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.imports.repository.ImportBatchRepository;
import ru.itmo.wmp.platform.application.imports.repository.ImportChangeRepository;
import ru.itmo.wmp.platform.application.organization.OrganizationService;
import ru.itmo.wmp.platform.application.organization.ParticipantExclusionService;
import ru.itmo.wmp.platform.application.organization.repository.BlockMemberRepository;
import ru.itmo.wmp.platform.application.organization.repository.BlockRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.security.UserAuthorizationService;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.imports.*;
import ru.itmo.wmp.platform.domain.organization.*;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.imports.ImportBatchNotFoundException;
import ru.itmo.wmp.platform.exception.imports.ImportNotApplicableException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantImportService {
    private final ParticipantListParser parser;
    private final ParticipantImportValidator validator;
    private final ImportBatchRepository batchRepository;
    private final ImportChangeRepository changeRepository;
    private final OrganizationService organizationService;
    private final OrganizationParticipantRepository participantRepository;
    private final BlockRepository blockRepository;
    private final BlockMemberRepository blockMemberRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ParticipantExclusionService participantExclusionService;
    private final UserAuthorizationService userAuthorizationService;

    public ImportBatch upload(
        Long organizationId,
        String filename,
        byte[] content,
        Long actorUserId
    ) {
        Organization organization = organizationService.getById(organizationId);

        List<ParsedParticipantRow> rows = parser.parse(filename, content);

        Map<String, Long> blocksByName = blocksByName(organizationId);

        Map<String, OrganizationParticipant> existing = participantRepository
            .findByOrganizationId(organizationId)
            .stream()
            .collect(Collectors.toMap(OrganizationParticipant::getIsuNumber, participant -> participant));

        ImportBatch batch = new ImportBatch();

        batch.setOrganization(organization);
        batch.setFilename(filename);
        batch.setStatus(ImportBatchStatus.VALIDATED);
        batch.setTotalRows(rows.size());
        batch.setCreatedBy(userService.getById(actorUserId));

        ImportBatch savedBatch = batchRepository.save(batch);

        List<ImportChange> changes = new ArrayList<>();
        List<String> seenIsuNumbers = new ArrayList<>();

        for (ParsedParticipantRow row : rows) {
            List<String> errors = validator.validate(row, blocksByName, seenIsuNumbers);

            if (!errors.isEmpty()) {
                changes.add(change(savedBatch, row, ImportChangeType.ERROR, String.join("; ", errors), null));

                continue;
            }

            seenIsuNumbers.add(row.isuNumber());

            OrganizationParticipant participant = existing.get(row.isuNumber());

            if (participant == null) {
                changes.add(change(savedBatch, row, ImportChangeType.CREATE, created(row), null));

                continue;
            }

            String difference = difference(participant, row);

            changes.add(change(
                savedBatch,
                row,
                difference.isEmpty() ? ImportChangeType.UNCHANGED : ImportChangeType.UPDATE,
                difference.isEmpty() ? null : difference,
                participant
            ));
        }

        Set<Long> protectedUsers = protectedUsers(organizationId, actorUserId);

        existing.values().stream()
            .filter(participant -> participant.getStatus() == ParticipantStatus.ACTIVE)
            .filter(participant -> !seenIsuNumbers.contains(participant.getIsuNumber()))
            .filter(participant -> !isProtected(participant, protectedUsers))
            .forEach(participant -> changes.add(
                deactivation(savedBatch, participant)
            ));

        int errorCount = (int) changes.stream()
            .filter(change -> change.getType() == ImportChangeType.ERROR)
            .count();

        savedBatch.setErrorCount(errorCount);
        savedBatch.setStatus(errorCount > 0 ? ImportBatchStatus.FAILED : ImportBatchStatus.VALIDATED);

        changeRepository.saveAll(changes);

        return batchRepository.save(savedBatch);
    }

    @Transactional(readOnly = true)
    public ImportBatch getBatch(Long organizationId, Long batchId) {
        ImportBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ImportBatchNotFoundException(organizationId, batchId));

        if (!Objects.equals(batch.getOrganization().getId(), organizationId)) {
            throw new ImportBatchNotFoundException(organizationId, batchId);
        }

        return batch;
    }

    @Transactional(readOnly = true)
    public List<ImportChange> changesOf(Long batchId) {
        return changeRepository.findByBatchIdOrderByIdAsc(batchId);
    }

    @Transactional(readOnly = true)
    public List<ImportBatch> history(Long organizationId) {
        organizationService.getById(organizationId);

        return batchRepository.findByOrganizationIdOrderByIdDesc(organizationId);
    }

    public ImportBatch apply(Long organizationId, Long batchId, Long actorUserId) {
        ImportBatch batch = getBatch(organizationId, batchId);

        if (!batch.isApplicable()) {
            throw new ImportNotApplicableException(batchId, batch.getStatus().name());
        }

        Map<String, Long> blocksByName = blocksByName(organizationId);

        for (ImportChange change : changesOf(batchId)) {
            switch (change.getType()) {
                case CREATE -> applyCreate(batch.getOrganization(), change, blocksByName, actorUserId);
                case UPDATE -> applyUpdate(organizationId, change, blocksByName, actorUserId);
                case DEACTIVATE -> participantExclusionService.exclude(
                    organizationId,
                    change.getIsuNumber()
                );
                default -> {
                }
            }
        }

        batch.setStatus(ImportBatchStatus.APPLIED);
        batch.setAppliedAt(LocalDateTime.now());
        batch.setAppliedBy(userService.getById(actorUserId));

        return batchRepository.save(batch);
    }

    public ImportBatch cancel(Long organizationId, Long batchId) {
        ImportBatch batch = getBatch(organizationId, batchId);

        if (batch.getStatus() == ImportBatchStatus.APPLIED) {
            throw new ImportNotApplicableException(batchId, batch.getStatus().name());
        }

        batch.setStatus(ImportBatchStatus.CANCELLED);

        return batchRepository.save(batch);
    }

    private void applyCreate(
        Organization organization,
        ImportChange change,
        Map<String, Long> blocksByName,
        Long actorUserId
    ) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organization.getId(), change.getIsuNumber())
            .orElseGet(OrganizationParticipant::new);

        participant.setOrganization(organization);
        participant.setIsuNumber(change.getIsuNumber());
        participant.setUser(userRepository.findByIsuNumber(change.getIsuNumber()).orElse(null));

        applyFields(participant, change);

        OrganizationParticipant saved = participantRepository.save(participant);

        evictParticipant(saved);

        syncBlocks(saved, change, blocksByName, actorUserId);
    }

    private void applyUpdate(
        Long organizationId,
        ImportChange change,
        Map<String, Long> blocksByName,
        Long actorUserId
    ) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organizationId, change.getIsuNumber())
            .orElse(null);

        if (participant == null) {
            return;
        }

        applyFields(participant, change);

        OrganizationParticipant saved = participantRepository.save(participant);

        evictParticipant(saved);

        syncBlocks(saved, change, blocksByName, actorUserId);
    }

    private void evictParticipant(OrganizationParticipant participant) {
        if (participant.getUser() != null) {
            userAuthorizationService.evict(participant.getUser().getId());
        }
    }

    private void applyFields(OrganizationParticipant participant, ImportChange change) {
        participant.setLastName(change.getLastName());
        participant.setFirstName(change.getFirstName());
        participant.setPatronymic(change.getPatronymic());
        participant.setEmail(change.getEmail());
        participant.setPosition(change.getPosition());
        participant.setStatus(ParticipantStatus.ACTIVE);
    }

    private void syncBlocks(
        OrganizationParticipant participant,
        ImportChange change,
        Map<String, Long> blocksByName,
        Long actorUserId
    ) {
        Set<String> target = targetBlocks(change);

        List<BlockMember> current = blockMemberRepository.findByParticipantId(participant.getId());

        List<BlockMember> obsolete = current.stream()
            .filter(member -> !target.contains(member.getBlock().getName().toLowerCase(Locale.ROOT)))
            .toList();

        blockMemberRepository.deleteAll(obsolete);

        Set<String> present = current.stream()
            .filter(member -> !obsolete.contains(member))
            .map(member -> member.getBlock().getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());

        target.stream()
            .filter(name -> !present.contains(name))
            .map(blocksByName::get)
            .filter(Objects::nonNull)
            .forEach(blockId -> addToBlock(participant, blockId, actorUserId));
    }

    private Set<String> targetBlocks(ImportChange change) {
        if (change.getBlockNames() == null || change.getBlockNames().isBlank()) {
            return Set.of();
        }

        return Arrays.stream(change.getBlockNames().split(","))
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void addToBlock(OrganizationParticipant participant, Long blockId, Long actorUserId) {
        blockRepository.findById(blockId).ifPresent(block -> {
            BlockMember member = new BlockMember();

            member.setBlock(block);
            member.setParticipant(participant);
            member.setAddedBy(userService.getById(actorUserId));

            blockMemberRepository.save(member);
        });
    }

    public ImportSummary summarize(Long batchId) {
        Map<ImportChangeType, Long> counts = changesOf(batchId).stream()
            .collect(Collectors.groupingBy(ImportChange::getType, Collectors.counting()));

        return new ImportSummary(
            count(counts, ImportChangeType.CREATE),
            count(counts, ImportChangeType.UPDATE),
            count(counts, ImportChangeType.DEACTIVATE),
            count(counts, ImportChangeType.UNCHANGED),
            count(counts, ImportChangeType.ERROR)
        );
    }

    private int count(Map<ImportChangeType, Long> counts, ImportChangeType type) {
        return counts.getOrDefault(type, 0L).intValue();
    }

    private Set<Long> protectedUsers(Long organizationId, Long actorUserId) {
        Set<Long> users = new HashSet<>();

        users.add(actorUserId);

        organizationService.managerOf(organizationId)
            .map(User::getId)
            .ifPresent(users::add);

        return users;
    }

    private boolean isProtected(OrganizationParticipant participant, Set<Long> protectedUsers) {
        return participant.getUser() != null && protectedUsers.contains(participant.getUser().getId());
    }

    private Map<String, Long> blocksByName(Long organizationId) {
        return blockRepository.findByOrganizationId(organizationId)
            .stream()
            .collect(Collectors.toMap(
                block -> block.getName().toLowerCase(Locale.ROOT),
                Block::getId,
                (first, second) -> first
            ));
    }

    private String created(ParsedParticipantRow row) {
        return row.blockNames().isEmpty()
            ? "новый участник"
            : "новый участник, блоки: " + String.join(", ", row.blockNames());
    }

    private String difference(OrganizationParticipant participant, ParsedParticipantRow row) {
        List<String> differences = new ArrayList<>();

        compare(differences, "фамилия", participant.getLastName(), row.lastName());
        compare(differences, "имя", participant.getFirstName(), row.firstName());
        compare(differences, "отчество", participant.getPatronymic(), row.patronymic());
        compare(differences, "email", participant.getEmail(), row.email());
        compare(differences, "должность", participant.getPosition(), row.position());

        if (participant.getStatus() != ParticipantStatus.ACTIVE) {
            differences.add("статус: возврат в организацию");
        }

        Set<String> current = currentBlockNames(participant.getId());
        Set<String> target = row.blockNames().stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        target.stream()
            .filter(block -> !current.contains(block))
            .forEach(block -> differences.add("блок +" + block));

        current.stream()
            .filter(block -> !target.contains(block))
            .forEach(block -> differences.add("блок -" + block));

        return String.join("; ", differences);
    }

    private Set<String> currentBlockNames(Long participantId) {
        return blockMemberRepository.findByParticipantId(participantId)
            .stream()
            .map(member -> member.getBlock().getName().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void compare(List<String> differences, String field, String current, String next) {
        String currentValue = current == null ? "" : current;
        String nextValue = next == null ? "" : next;

        if (!currentValue.equals(nextValue)) {
            differences.add(field + ": " + display(current) + " → " + display(next));
        }
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "пусто" : value;
    }

    private ImportChange change(
        ImportBatch batch,
        ParsedParticipantRow row,
        ImportChangeType type,
        String details,
        OrganizationParticipant participant
    ) {
        ImportChange change = new ImportChange();

        change.setBatch(batch);
        change.setRowNumber(row.rowNumber());
        change.setType(type);
        change.setIsuNumber(row.isuNumber());
        change.setLastName(row.lastName());
        change.setFirstName(row.firstName());
        change.setPatronymic(row.patronymic());
        change.setEmail(row.email());
        change.setPosition(row.position());
        change.setBlockNames(String.join(", ", row.blockNames()));
        change.setDetails(details);
        change.setParticipant(participant);

        return change;
    }

    private ImportChange deactivation(ImportBatch batch, OrganizationParticipant participant) {
        ImportChange change = new ImportChange();

        change.setBatch(batch);
        change.setType(ImportChangeType.DEACTIVATE);
        change.setIsuNumber(participant.getIsuNumber());
        change.setLastName(participant.getLastName());
        change.setFirstName(participant.getFirstName());
        change.setPatronymic(participant.getPatronymic());
        change.setEmail(participant.getEmail());
        change.setPosition(participant.getPosition());
        change.setDetails("отсутствует в файле, членство будет прекращено");
        change.setParticipant(participant);

        return change;
    }
}
