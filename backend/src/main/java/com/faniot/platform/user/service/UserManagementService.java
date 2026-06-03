package com.faniot.platform.user.service;

import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.system.service.SystemLogService;
import com.faniot.platform.user.domain.Role;
import com.faniot.platform.user.domain.User;
import com.faniot.platform.user.dto.UserRequest;
import com.faniot.platform.user.repository.RoleRepository;
import com.faniot.platform.user.repository.UserRepository;
import com.faniot.platform.user.vo.RoleVO;
import com.faniot.platform.user.vo.UserVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;

    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            SystemLogService systemLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserVO> page(String keyword, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(userRepository.findAll(buildSpec(keyword, status), pageable).map(this::toVO));
    }

    @Transactional(readOnly = true)
    public List<RoleVO> roles() {
        return roleRepository.findByEnabledTrueOrderByIdAsc().stream().map(RoleVO::from).toList();
    }

    @Transactional
    public UserVO create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessException("新增用户必须填写密码");
        }
        User user = new User();
        apply(user, request, true);
        User saved = userRepository.save(user);
        saveRoles(saved.getId(), request.roleCodes());
        systemLogService.record("用户管理", "新增用户", "新增用户：" + saved.getUsername());
        return toVO(saved);
    }

    @Transactional
    public UserVO update(Long id, UserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }
        apply(user, request, false);
        User saved = userRepository.save(user);
        saveRoles(saved.getId(), request.roleCodes());
        systemLogService.record("用户管理", "编辑用户", "编辑用户：" + saved.getUsername());
        return toVO(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
        jdbcTemplate.update("delete from user_roles where user_id = ?", id);
        userRepository.delete(user);
        systemLogService.record("用户管理", "删除用户", "删除用户：" + user.getUsername());
    }

    private void apply(User user, UserRequest request, boolean create) {
        user.setUsername(request.username());
        if (create || StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setStatus(StringUtils.hasText(request.status()) ? request.status() : "enabled");
    }

    private void saveRoles(Long userId, List<String> roleCodes) {
        jdbcTemplate.update("delete from user_roles where user_id = ?", userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        for (String roleCode : roleCodes) {
            Role role = roleRepository.findByRoleCode(roleCode).orElseThrow(() -> new BusinessException("角色不存在：" + roleCode));
            jdbcTemplate.update("insert into user_roles(user_id, role_id) values (?, ?) on conflict do nothing", userId, role.getId());
        }
    }

    private UserVO toVO(User user) {
        List<RoleVO> roles = jdbcTemplate.query("""
                        select r.id, r.role_code, r.role_name, r.description, r.enabled
                        from roles r
                        join user_roles ur on ur.role_id = r.id
                        where ur.user_id = ?
                        order by r.id
                        """,
                (rs, rowNum) -> new RoleVO(
                        rs.getLong("id"),
                        rs.getString("role_code"),
                        rs.getString("role_name"),
                        rs.getString("description"),
                        rs.getBoolean("enabled")
                ),
                user.getId()
        );
        return UserVO.from(
                user,
                roles.stream().map(RoleVO::roleCode).toList(),
                roles.stream().map(RoleVO::roleName).toList()
        );
    }

    private Specification<User> buildSpec(String keyword, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), like),
                        cb.like(root.get("realName"), like),
                        cb.like(root.get("phone"), like),
                        cb.like(root.get("email"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
