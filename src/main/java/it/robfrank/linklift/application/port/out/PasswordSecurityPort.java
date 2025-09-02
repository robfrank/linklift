package it.robfrank.linklift.application.port.out;

/**
 * Port interface for password security operations.
 * Provides abstraction for password hashing and verification.
 */
public interface PasswordSecurityPort {

    /**
     * Hashes a plain text password with a generated salt.
     *
     * @param plainPassword the plain text password to hash
     * @return PasswordHash containing the hash and salt
     */
    PasswordHash hashPassword(String plainPassword);

    /**
     * Verifies a plain text password against a stored hash.
     *
     * @param plainPassword the plain text password to verify
     * @param storedHash the stored password hash
     * @param salt the salt used for hashing
     * @return true if the password matches, false otherwise
     */
    boolean verifyPassword(String plainPassword, String storedHash, String salt);

    /**
     * Validates password strength according to security policy.
     *
     * @param password the password to validate
     * @return true if password meets requirements, false otherwise
     */
    boolean isPasswordStrong(String password);

    /**
     * Container for password hash and salt.
     */
    record PasswordHash(String hash, String salt) {}
}
