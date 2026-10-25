package util;

/**
 * Validation for the account name that crosses the HTTP trust boundary.
 *
 * <p>A value taken straight from an HTTP parameter is untrusted. Putting it into the HTTP
 * session mixes trusted and untrusted data in the same structure, so every later reader of
 * the session has to guess whether the value was ever checked - a Trust Boundary Violation
 * (CWE-501). The login servlet therefore validates the submitted name here before it
 * authenticates, and stores the name read back from the authenticated account record.</p>
 *
 * <p>The accepted form is an allowlist: letters or digits plus the separators
 * {@code . - _ @}, bounded by the width of the {@code accounts.name} column. Anything else
 * (control characters, line breaks, markup, an over-long value) fails closed.</p>
 *
 * <p>Implemented without extra dependencies so the plain servlet/JSP webapp keeps its
 * current dependency set.</p>
 */
public final class Usernames {

  /**
   * Maximum length of an account name, matching the {@code accounts.name varchar(32)}
   * column created by the register servlet.
   */
  public static final int MAX_LENGTH = 32;

  private Usernames() {
    // utility class
  }

  /**
   * Tells whether a candidate account name has the expected canonical form.
   *
   * <p>Fails closed: a null, empty, over-long or otherwise unexpected value is rejected.</p>
   *
   * @param candidate account name received from the request or read from the account store,
   *     may be null
   * @return true only when the value is present and consists solely of allowed characters
   */
  public static boolean isValid(String candidate) {
    if (candidate == null || candidate.isEmpty() || candidate.length() > MAX_LENGTH) {
      return false;
    }
    for (int i = 0; i < candidate.length(); i++) {
      if (!isAllowed(candidate.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isAllowed(char character) {
    if (Character.isLetterOrDigit(character)) {
      return true;
    }
    return character == '.' || character == '-' || character == '_' || character == '@';
  }
}
