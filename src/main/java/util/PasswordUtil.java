package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256 with per-user salt.
 */
public class PasswordUtil {
  private static final String ALGORITHM = "SHA-256";
  private static final int SALT_LENGTH = 16;

  /**
   * Generates a random salt for this user.
   *
   * @return Base64-encoded salt
   */
  public static String generateSalt() {
    SecureRandom random = new SecureRandom();
    byte[] saltBytes = new byte[SALT_LENGTH];
    random.nextBytes(saltBytes);
    return Base64.getEncoder().encodeToString(saltBytes);
  }

  /**
   * Hashes a password with the provided salt using SHA-256.
   *
   * @param password The plaintext password
   * @param salt The Base64-encoded salt
   * @return Base64-encoded hash
   */
  public static String hashPassword(String password, String salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
      byte[] saltBytes = Base64.getDecoder().decode(salt);
      digest.update(saltBytes);
      byte[] hashedBytes = digest.digest(password.getBytes());
      return Base64.getEncoder().encodeToString(hashedBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Verifies a plaintext password against a stored hash and salt.
   *
   * @param plaintext The plaintext password provided by the user
   * @param storedHash The Base64-encoded stored hash
   * @param salt The Base64-encoded salt
   * @return true if the password matches, false otherwise
   */
  public static boolean verifyPassword(String plaintext, String storedHash, String salt) {
    String computedHash = hashPassword(plaintext, salt);
    return constantTimeEquals(computedHash, storedHash);
  }

  /**
   * Constant-time string comparison to prevent timing attacks.
   *
   * @param a First string
   * @param b Second string
   * @return true if equal, false otherwise
   */
  private static boolean constantTimeEquals(String a, String b) {
    byte[] aBytes = a.getBytes();
    byte[] bBytes = b.getBytes();

    if (aBytes.length != bBytes.length) {
      return false;
    }

    int result = 0;
    for (int i = 0; i < aBytes.length; i++) {
      result |= aBytes[i] ^ bBytes[i];
    }

    return result == 0;
  }
}
