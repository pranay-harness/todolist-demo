package db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Account passwords as they are held in the {@code accounts} table.
 *
 * <p>A password is never stored in a recoverable form: {@link #hash(String)} derives a PBKDF2
 * (HMAC-SHA256) key from the password using a fresh random salt and returns a self describing
 * value that records the algorithm, the iteration count, the salt and the derived key:
 *
 * <pre>pbkdf2_sha256$&lt;iterations&gt;$&lt;base64 salt&gt;$&lt;base64 derived key&gt;</pre>
 *
 * <p>{@link #matches(String, String)} re-derives the key with the parameters recorded in the
 * stored value and compares the two keys in constant time, so verifying a login leaks neither the
 * stored key nor how much of it was correct. Anything that is not a value produced by
 * {@link #hash(String)} (for example a password left over in plaintext by an older build) fails
 * closed and never matches, rather than falling back to comparing cleartext.
 *
 * <p>The encoded value is about 90 characters long, which is why the {@code password} column is
 * declared wide enough to hold it.
 */
public final class StoredPassword {
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String PREFIX = "pbkdf2_sha256";
  /** Cost of one derivation. Deliberately high so that guessing a stolen hash stays expensive. */
  private static final int ITERATIONS = 210000;
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  private StoredPassword() {
  }

  /**
   * Returns the value to persist for the given cleartext password.
   */
  public static String hash(String password) {
    if (password == null) {
      throw new IllegalArgumentException("A password is required");
    }
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    byte[] derivedKey = deriveKey(password, salt, ITERATIONS);
    return PREFIX + "$" + ITERATIONS + "$" + encode(salt) + "$" + encode(derivedKey);
  }

  /**
   * Returns true only when the supplied cleartext password produced the stored value.
   */
  public static boolean matches(String storedPassword, String suppliedPassword) {
    if (storedPassword == null || suppliedPassword == null) {
      return false;
    }
    String[] parts = storedPassword.split("\\$", -1);
    if (parts.length != 4 || !PREFIX.equals(parts[0])) {
      return false;
    }
    int iterations;
    byte[] salt;
    byte[] expectedKey;
    try {
      iterations = Integer.parseInt(parts[1]);
      salt = Base64.getDecoder().decode(parts[2]);
      expectedKey = Base64.getDecoder().decode(parts[3]);
    } catch (IllegalArgumentException e) {
      // Not a value this class produced, so there is nothing to verify against.
      return false;
    }
    if (iterations <= 0 || salt.length == 0 || expectedKey.length == 0) {
      return false;
    }
    byte[] actualKey = deriveKey(suppliedPassword, salt, iterations);
    return MessageDigest.isEqual(expectedKey, actualKey);
  }

  private static byte[] deriveKey(String password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("The " + ALGORITHM + " key derivation function is unavailable", e);
    } catch (InvalidKeySpecException e) {
      throw new IllegalStateException("Unable to derive a key from the supplied password", e);
    } finally {
      spec.clearPassword();
    }
  }

  private static String encode(byte[] value) {
    return Base64.getEncoder().encodeToString(value);
  }
}
