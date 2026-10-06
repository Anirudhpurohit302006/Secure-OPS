package com.secureops.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Server-side authentication boundary: every /api/** route except login/logout needs a valid session. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String SESSION_USER = "secureops.user";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        if ("OPTIONS".equals(req.getMethod())) return true;
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER) != null) return true;
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"UNAUTHENTICATED\",\"message\":\"Sign in required.\"}");
        return false;
    }
}
