package ru.itmo.wmp.platform.exception.organization;

public class BlockNameAlreadyUsedException extends RuntimeException {
    public BlockNameAlreadyUsedException(Long organizationId, String name) {
        super("Block " + name + " already exists in organization id=" + organizationId);
    }
}
