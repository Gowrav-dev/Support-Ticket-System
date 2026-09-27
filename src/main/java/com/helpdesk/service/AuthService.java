package com.helpdesk.service;

import com.helpdesk.dao.UserDao;
import com.helpdesk.model.User;
import com.helpdesk.util.PasswordUtil;
import com.helpdesk.util.SessionManager;

import java.sql.SQLException;

/**
 * Service class for User and Support Staff authentication and registration.
 */
public class AuthService {

    private final UserDao userDao = new UserDao();

    public static class AuthResult {
        public String token;
        public User user;

        public AuthResult(String token, User user) {
            this.token = token;
            this.user = user;
        }
    }

    public User register(String username, String password, String fullName, String role) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = username.trim();
        }

        // Validate role: only "USER" or "SUPPORT_STAFF"
        String normalizedRole = "USER";
        if (role != null && (role.equalsIgnoreCase("SUPPORT_STAFF") || role.equalsIgnoreCase("STAFF") || role.equalsIgnoreCase("ADMIN"))) {
            normalizedRole = "SUPPORT_STAFF";
        }

        String cleanUsername = username.trim().toLowerCase();

        // Check if user exists
        User existing = userDao.findByUsername(cleanUsername);
        if (existing != null) {
            throw new IllegalArgumentException("Username '" + cleanUsername + "' is already taken");
        }

        String passwordHash = PasswordUtil.hash(password);
        User newUser = new User(0, cleanUsername, passwordHash, fullName.trim(), normalizedRole);

        return userDao.createUser(newUser);
    }

    public AuthResult login(String username, String password) throws Exception {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password are required");
        }

        String cleanUsername = username.trim().toLowerCase();
        User user = userDao.findByUsername(cleanUsername);

        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = SessionManager.createSession(user);
        return new AuthResult(token, user);
    }
}
