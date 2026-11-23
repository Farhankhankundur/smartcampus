package com.campus.service;

import com.campus.exception.Errors;
import com.campus.model.AppUser;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** Registration, login and sessions. Passwords are stored as BCrypt hashes, never as plain text. */
@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    // token -> signed-in user. In memory: restarting the server signs everyone out.
    private final Map<String, AppUser> sessions = new ConcurrentHashMap<>();

    public AuthService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public synchronized AppUser register(String username, String password, String fullName, String role) {
        String u = username == null ? "" : username.trim().toLowerCase();
        if (!u.matches("[a-z0-9_]{3,30}"))
            throw new IllegalArgumentException("Username must be 3 to 30 letters, numbers or underscores.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Full name is required.");
        Integer taken = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?", Integer.class, u);
        if (taken != null && taken > 0) throw new Errors.Unavailable("That username is already taken.");
        jdbc.update("INSERT INTO users(username,password_hash,full_name,role) VALUES (?,?,?,?)",
                u, encoder.encode(password), fullName.trim(), role);
        return jdbc.queryForObject("SELECT id, username, full_name, role FROM users WHERE username=?",
                (rs, i) -> new AppUser(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role")), u);
    }

    /** Checks the password and returns a new session token. */
    public String login(String username, String password) {
        String u = username == null ? "" : username.trim().toLowerCase();
        List<Object[]> rows = jdbc.query(
                "SELECT id, username, full_name, role, password_hash FROM users WHERE username=?",
                (rs, i) -> new Object[]{
                        new AppUser(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"), rs.getString("role")),
                        rs.getString("password_hash")},
                u);
        if (rows.isEmpty() || !encoder.matches(password == null ? "" : password, (String) rows.get(0)[1]))
            throw new Errors.Unauthorized("Wrong username or password.");
        byte[] raw = new byte[24];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        sessions.put(token, (AppUser) rows.get(0)[0]);
        return token;
    }

    public AppUser userForToken(String token) { return token == null ? null : sessions.get(token); }

    public void logout(String token) { if (token != null) sessions.remove(token); }

    /** Demo accounts, created only when there are no users yet. */
    public void seedDefaultUsers() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (n != null && n > 0) return;
        register("admin", "admin123", "Administrator", "ADMIN");
        register("student", "student123", "Rahul Sharma", "STUDENT");
    }
}
