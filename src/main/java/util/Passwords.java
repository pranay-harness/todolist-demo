package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Password comparison helpers shared by the authentication servlets.
 *
 * <p>{@link String#equals(Object)} returns as soon as it finds the first differing
 * character, so the time it takes to reject a candidate password reveals how many leading
 * characters were correct. An attacker can measure that difference and recover the secret
 * one character at a time, which is an Observable Timing Discrepancy (CWE-208).</p>
 *
 * <p>{@link MessageDigest#isEqual(byte[], byte[])} compares every byte before returning,
 * so the comparison takes the same time whichever byte differs.</p>
 *
 * <p>Implemented without extra dependencies so the plain servlet/JSP webapp keeps its
 * current dependency set.</p>
 */
public final class Passwords {

  private Passwords() {
    // utility class
  }

  /**
   * Compares a supplied password with the stored one in constant time.
   *
   * <p>Fails closed: a null or absent value on either side never authenticates.</p>
   *
   * @param supplied password received from the request, may be null
   * @param stored password read from the account store, may be null
   * @return true only when both values are present and equal
   */
  public static boolean matches(String supplied, String stored) {
    if (supplied == null || stored == null) {
      return false;
    }
    return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
        stored.getBytes(StandardCharsets.UTF_8));
  }
}
