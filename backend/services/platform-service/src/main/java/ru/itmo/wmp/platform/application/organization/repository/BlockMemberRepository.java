package ru.itmo.wmp.platform.application.organization.repository;

import ru.itmo.wmp.platform.domain.organization.BlockMember;

import java.util.List;
import java.util.Optional;

public interface BlockMemberRepository {
    List<BlockMember> findByBlockId(Long blockId);

    List<BlockMember> findByParticipantId(Long participantId);

    Optional<BlockMember> findByBlockIdAndParticipantId(Long blockId, Long participantId);

    BlockMember save(BlockMember blockMember);

    void delete(BlockMember blockMember);

    void deleteAll(List<BlockMember> blockMembers);
}
