package ru.itmo.wmp.platform.api.organization.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.organization.Block;
import ru.itmo.wmp.platform.domain.organization.BlockRole;
import ru.itmo.wmp.platform.dto.organization.BlockResponse;
import ru.itmo.wmp.platform.dto.organization.BlockRoleResponse;

@Component
public class BlockMapper {
    public BlockResponse toResponse(Block block, String managerIsuNumber) {
        return new BlockResponse(
            block.getId(),
            block.getOrganization().getId(),
            block.getName(),
            block.getStatus(),
            managerIsuNumber,
            block.getCreatedAt()
        );
    }

    public BlockRoleResponse toResponse(BlockRole role) {
        return new BlockRoleResponse(
            role.getBlock().getId(),
            role.getUser().getId(),
            role.getUser().getIsuNumber(),
            role.getUser().fullName(),
            role.getRole(),
            role.getGrantedAt()
        );
    }
}
