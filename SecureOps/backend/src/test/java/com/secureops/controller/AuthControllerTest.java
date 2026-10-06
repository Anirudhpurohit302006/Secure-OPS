package com.secureops.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.secureops.config.AppProperties;
import com.secureops.security.AuthInterceptor;
import com.secureops.security.LoginThrottle;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.handler.MappedInterceptor;

class AuthControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        String hash = new BCryptPasswordEncoder().encode("s3cret-pass");
        var props = new AppProperties(null, new AppProperties.Auth("admin", hash), null, List.of(), null);
        var interceptor = new MappedInterceptor(new String[] {"/api/**"},
                new String[] {"/api/auth/login", "/api/auth/logout"}, new AuthInterceptor());
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(props, new LoginThrottle()))
                .addInterceptors(interceptor).build();
    }

    private static String creds(String u, String p) { return "{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}"; }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds("admin", "nope")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void wrongUsernameIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds("root", "s3cret-pass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionRequiresLogin() throws Exception {
        mvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginCreatesSessionAndLogoutDestroysIt() throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds("admin", "s3cret-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"));

        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/session").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailuresLockTheClient() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds("admin", "bad")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds("admin", "s3cret-pass")))
                .andExpect(status().isTooManyRequests());
    }
}
