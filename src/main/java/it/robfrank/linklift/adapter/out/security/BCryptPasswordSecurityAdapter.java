package it.robfrank.linklift.adapter.out.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import it.robfrank.linklift.application.port.out.PasswordSecurityPort;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Infrastructure adapter implementing password security using BCrypt.
 */
public class BCryptPasswordSecurityAdapter implements PasswordSecurityPort {

  private static final int BCRYPT_COST = 12; // Strong cost factor
  private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits

  // Password strength requirements
  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 128;
  private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
  private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
  private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
  private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

  private final SecureRandom secureRandom;

  public BCryptPasswordSecurityAdapter() {
    this.secureRandom = new SecureRandom();
  }

  @Override
  public PasswordHash hashPassword(String plainPassword) {
    // Generate a random salt
    byte[] saltBytes = new byte[SALT_LENGTH];
    secureRandom.nextBytes(saltBytes);
    String salt = Base64.getEncoder().encodeToString(saltBytes);

    // Combine password and salt for hashing
    String saltedPassword = plainPassword + salt;

    // Hash with BCrypt
    String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, saltedPassword.toCharArray());

    return new PasswordHash(hash, salt);
  }

  @Override
  public boolean verifyPassword(String plainPassword, String storedHash, String salt) {
    if (plainPassword == null || storedHash == null || salt == null) {
      return false;
    }

    try {
      // Combine password and salt (same as during hashing)
      String saltedPassword = plainPassword + salt;

      // Verify with BCrypt
      BCrypt.Result result = BCrypt.verifyer().verify(saltedPassword.toCharArray(), storedHash);
      return result.verified;
    } catch (Exception e) {
      // Any exception during verification means failure
      return false;
    }
  }

  @Override
  public boolean isPasswordStrong(String password) {
    if (password == null) {
      return false;
    }

    // Check length
    if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
      return false;
    }

    // Check for required character types
    boolean hasUpper = HAS_UPPER.matcher(password).find();
    boolean hasLower = HAS_LOWER.matcher(password).find();
    boolean hasDigit = HAS_DIGIT.matcher(password).find();
    boolean hasSpecial = HAS_SPECIAL.matcher(password).find();

    // Require at least 3 of the 4 character types
    int characterTypeCount = 0;
    if (hasUpper) characterTypeCount++;
    if (hasLower) characterTypeCount++;
    if (hasDigit) characterTypeCount++;
    if (hasSpecial) characterTypeCount++;

    return characterTypeCount >= 3;
  }
}
