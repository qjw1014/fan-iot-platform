package com.faniot.platform.user.repository;

import com.faniot.platform.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCode(String roleCode);

    List<Role> findByEnabledTrueOrderByIdAsc();
}
