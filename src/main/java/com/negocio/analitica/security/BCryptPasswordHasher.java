package com.negocio.analitica.security;

import org.mindrot.jbcrypt.BCrypt;
import java.nio.charset.StandardCharsets;

/** BCrypt password hashing with a work factor suitable for this application. */
public final class BCryptPasswordHasher implements PasswordHasher {
    private static final int WORK_FACTOR = 12;
    private static final int MAX_BCRYPT_BYTES = 72;

    @Override
    public String hash(char[] password) {
        validatePassword(password);
        String value = new String(password);
        ensureSupportedLength(value);
        return BCrypt.hashpw(value, BCrypt.gensalt(WORK_FACTOR));
    }

    @Override
    public boolean verify(char[] password, String encodedHash) {
        if (password == null || password.length == 0 || encodedHash == null
                || encodedHash.isBlank()) {
            return false;
        }
        String value = new String(password);
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            return false;
        }
        try {
            return BCrypt.checkpw(value, encodedHash);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < 10) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 10 caracteres.");
        }
    }

    private static void ensureSupportedLength(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalArgumentException(
                    "La contraseña supera el límite admitido por BCrypt (72 bytes UTF-8).");
        }
    }
}
