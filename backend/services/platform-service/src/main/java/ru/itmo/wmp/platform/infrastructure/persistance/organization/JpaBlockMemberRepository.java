package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.BlockMember;

import java.util.List;
import java.util.Optional;

public interface JpaBlockMemberRepository extends JpaRepository<BlockMember, Long> {
    List<BlockMember> findByBlockId(Long blockId);

    List<BlockMember> findByParticipantId(Long participantId);

    Optional<BlockMember> findByBlockIdAndParticipantId(Long blockId, Long participantId);
}
