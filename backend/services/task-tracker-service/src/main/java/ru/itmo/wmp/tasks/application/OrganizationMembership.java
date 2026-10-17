package ru.itmo.wmp.tasks.application;

import java.util.List;
import java.util.Set;

public record OrganizationMembership(
    Long organizationId,
    Long userId,
    boolean platformAdmin,
    boolean member,
    Set<String> roles,
    List<BlockMembership> blocks
) {
    public record BlockMembership(
        Long blockId,
        Set<String> roles
    ) {
    }

    public OrganizationMembership(
        Long organizationId,
        Long userId,
        boolean platformAdmin,
        boolean member,
        Set<String> roles,
        List<BlockMembership> blocks
    ) {
        this.organizationId = organizationId;
        this.userId = userId;
        this.platformAdmin = platformAdmin;
        this.member = member;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
        this.blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    public boolean canManageOrganization() {
        return platformAdmin || roles.contains("MANAGER") || roles.contains("ADMIN");
    }

    public boolean managesBlock(Long blockId) {
        if (blockId == null) {
            return false;
        }

        return blocks.stream()
            .anyMatch(block -> block.blockId().equals(blockId) && block.roles().contains("MANAGER"));
    }

    public List<Long> managedBlocks() {
        return blocks.stream()
            .filter(block -> block.roles().contains("MANAGER"))
            .map(BlockMembership::blockId)
            .toList();
    }

    public boolean hasAccess() {
        return platformAdmin || member || canManageOrganization();
    }
}
