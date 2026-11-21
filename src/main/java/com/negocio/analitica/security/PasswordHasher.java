package com.negocio.analitica.security;

/** Defines secure password hashing and verification operations. */
public interface PasswordHasher {
    String hash(char[] password);
    boolean verify(char[] password, String encodedHash);
}
