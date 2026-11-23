package com.campus.config;

import com.campus.model.AppUser;
import com.campus.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Runs before every /api request: is the user signed in, and are they allowed to do this? */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    // Only an administrator may change these (everyone signed in may view them).
    private static final List<String> ADMIN_ONLY_WRITES = List.of(
            "/api/students", "/api/faculty", "/api/courses", "/api/resources", "/api/events", "/api/books");

    private final AuthService auth;
    public AuthInterceptor(AuthService auth) { this.auth = auth; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        if ("OPTIONS".equals(req.getMethod())) return true;
        String path = req.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) return true;

        String header = req.getHeader("Authorization");
        AppUser user = (header != null && header.startsWith("Bearer ")) ? auth.userForToken(header.substring(7)) : null;
        if (user == null) return deny(res, 401, "Please sign in to continue.");
        req.setAttribute("user", user);

        boolean isWrite = !"GET".equals(req.getMethod());
        boolean adminOnly = ADMIN_ONLY_WRITES.stream().anyMatch(path::startsWith);
        if (isWrite && adminOnly && !user.isAdmin()) return deny(res, 403, "Only an administrator can change this.");
        return true;
    }

    private boolean deny(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
