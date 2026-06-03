package com.faniot.platform.bootstrap;

import com.faniot.platform.user.domain.User;
import com.faniot.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String realName;

    public AdminUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.real-name:系统管理员}") String realName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.realName = realName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.info("未配置ADMIN_USERNAME和ADMIN_PASSWORD，跳过管理员账号初始化");
            return;
        }
        if (userRepository.existsByUsername(username)) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setStatus("enabled");
        userRepository.save(user);
        log.info("管理员账号初始化完成: {}", username);
    }
}
