package util;

import junit.framework.TestCase;

/**
 * Unit tests for {@link Passwords}, the constant-time password comparison used by the
 * login servlet.
 *
 * <p>Written in the JUnit 3 style ({@code junit.framework.TestCase}) so it compiles and
 * runs with either JUnit version declared in the build.</p>
 */
public class PasswordsTest extends TestCase {

  public void testMatchesAcceptsTheCorrectPassword() {
    assertTrue(Passwords.matches("s3cret-pass", "s3cret-pass"));
  }

  public void testMatchesRejectsAWrongPassword() {
    assertFalse(Passwords.matches("s3cret-pasS", "s3cret-pass"));
    assertFalse(Passwords.matches("s3cret", "s3cret-pass"));
    assertFalse(Passwords.matches("s3cret-pass-longer", "s3cret-pass"));
  }

  public void testMatchesRejectsEmptyAgainstNonEmpty() {
    assertFalse(Passwords.matches("", "s3cret-pass"));
    assertFalse(Passwords.matches("s3cret-pass", ""));
  }

  public void testMatchesFailsClosedOnNull() {
    assertFalse(Passwords.matches(null, "s3cret-pass"));
    assertFalse(Passwords.matches("s3cret-pass", null));
    assertFalse(Passwords.matches(null, null));
  }

  public void testMatchesComparesNonAsciiCharactersByteForByte() {
    assertTrue(Passwords.matches("paßwort-é", "paßwort-é"));
    assertFalse(Passwords.matches("paßwort-é", "paßwort-e"));
  }
}
