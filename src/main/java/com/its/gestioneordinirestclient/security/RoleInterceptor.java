package com.its.gestioneordinirestclient.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final String ROLES_HEADER = "Auth-Roles";
    private static final String ROLE_ADMIN   = "ROLE_ADMIN";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) return true;
        if (!method.hasMethodAnnotation(RequiresAdmin.class)) return true;

        String rawHeader = request.getHeader(ROLES_HEADER);

        List<String> roles = parseRoles(rawHeader);

        if (roles.contains(ROLE_ADMIN)) return true;

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: ADMIN role required");
        return false;
    }

    private List<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return List.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}