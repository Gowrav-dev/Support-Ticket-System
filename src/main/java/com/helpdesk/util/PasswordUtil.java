package com.helpdesk.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility to hash and verify passwords using BCrypt.
 */
public class PasswordUtil {

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    public static boolean verify(String password, String hashed) {
        if (password == null || hashed == null) return false;
        try {
            return BCrypt.checkpw(password, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
