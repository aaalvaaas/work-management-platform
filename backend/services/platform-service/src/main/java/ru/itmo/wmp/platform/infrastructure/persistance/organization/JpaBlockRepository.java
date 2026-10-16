package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.Block;

import java.util.List;

public interface JpaBlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByOrganizationId(Long organizationId);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
