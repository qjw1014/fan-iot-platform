package com.faniot.platform.user.service;

import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.security.JwtService;
import com.faniot.platform.user.domain.User;
import com.faniot.platform.user.dto.LoginRequest;
import com.faniot.platform.user.dto.LoginResponse;
import com.faniot.platform.user.repository.UserRepository;
import com.faniot.platform.user.vo.UserVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!"enabled".equals(user.getStatus())) {
            throw new BusinessException(403, "用户已被禁用或锁定");
        }
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        return new LoginResponse(jwtService.generateToken(user.getUsername()), "Bearer", UserVO.from(user));
    }
}
