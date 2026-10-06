package com.secureops.controller;

import com.secureops.config.AppProperties;
import com.secureops.dto.Dtos.ApiError;
import com.secureops.dto.Dtos.LoginRequest;
import com.secureops.dto.Dtos.SessionUser;
import com.secureops.security.AuthInterceptor;
import com.secureops.security.LoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppProperties.Auth auth;
    private final LoginThrottle throttle;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(AppProperties props, LoginThrottle throttle) {
        this.auth = props.auth();
        this.throttle = throttle;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest req) {
        String client = req.getRemoteAddr();
        if (throttle.isLocked(client)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiError("TOO_MANY_ATTEMPTS", "Too many failed sign-in attempts. Try again in a few minutes."));
        }
        if (auth.passwordHash() == null || auth.passwordHash().isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("NOT_CONFIGURED", "Admin credentials are not configured (ADMIN_PASSWORD_HASH)."));
        }
        String user = body == null || body.username() == null ? "" : body.username();
        String pass = body == null || body.password() == null ? "" : body.password();
        // Always run BCrypt so timing does not reveal whether the username exists.
        boolean passwordOk = encoder.matches(pass, auth.passwordHash());
        boolean ok = passwordOk && user.equals(auth.username());
        if (!ok) {
            throttle.recordFailure(client);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("INVALID_CREDENTIALS", "Invalid username or password."));
        }
        throttle.reset(client);
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();                       // prevent session fixation
        HttpSession session = req.getSession(true);
        session.setAttribute(AuthInterceptor.SESSION_USER, user);
        return ResponseEntity.ok(new SessionUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    /** Reaches this method only with a valid session (the interceptor returns 401 otherwise). */
    @GetMapping("/session")
    public SessionUser session(HttpServletRequest req) {
        return new SessionUser((String) req.getSession(false).getAttribute(AuthInterceptor.SESSION_USER));
    }
}
