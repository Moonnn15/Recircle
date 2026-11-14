package com.recircle.security;

import com.recircle.entity.User;
import com.recircle.enums.AccountStatus;
import com.recircle.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                AuthPrincipal principal = jwtService.validateAndExtract(token);
                if (principal != null) {
                    Optional<User> userOpt = userRepository.findById(principal.getUserId());
                    if (userOpt.isPresent() && userOpt.get().getAccountStatus() == AccountStatus.ACTIVE) {
                        User dbUser = userOpt.get();
                        AuthContext.set(new AuthPrincipal(
                            dbUser.getId(),
                            dbUser.getEmail(),
                            dbUser.getRole(),
                            dbUser.isCampusVerified()
                        ));
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}
