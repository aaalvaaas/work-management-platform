package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.organization.repository.BlockMemberRepository;
import ru.itmo.wmp.platform.domain.organization.BlockMember;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BlockMemberRepositoryAdapter implements BlockMemberRepository {
    private final JpaBlockMemberRepository blockMemberRepository;

    @Override
    public List<BlockMember> findByBlockId(Long blockId) {
        return blockMemberRepository.findByBlockId(blockId);
    }

    @Override
    public List<BlockMember> findByParticipantId(Long participantId) {
        return blockMemberRepository.findByParticipantId(participantId);
    }

    @Override
    public Optional<BlockMember> findByBlockIdAndParticipantId(Long blockId, Long participantId) {
        return blockMemberRepository.findByBlockIdAndParticipantId(blockId, participantId);
    }

    @Override
    public BlockMember save(BlockMember blockMember) {
        return blockMemberRepository.save(blockMember);
    }

    @Override
    public void delete(BlockMember blockMember) {
        blockMemberRepository.delete(blockMember);
    }

    @Override
    public void deleteAll(List<BlockMember> blockMembers) {
        blockMemberRepository.deleteAll(blockMembers);
    }
}
