package ru.itmo.wmp.platform.exception.organization;

public class BlockNotFoundException extends RuntimeException {
    public BlockNotFoundException(Long id) {
        super("Block with id=" + id + " not found");
    }

    public BlockNotFoundException(Long organizationId, Long blockId) {
        super("Block id=" + blockId + " does not belong to organization id=" + organizationId);
    }
}
