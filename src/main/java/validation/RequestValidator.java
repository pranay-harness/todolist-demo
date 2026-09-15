package validation;

import java.util.regex.Pattern;

/**
 * Allowlists for the request parameters the servlets accept.
 *
 * <p>Every check is fail closed: {@code null}, empty and out-of-range values are rejected, so an
 * unvalidated request parameter never reaches the database, a redirect or the session. The bounds
 * mirror the schema created in {@code register.Register#init()} ({@code accounts.name varchar(32)},
 * {@code accounts.password varchar(32)}, {@code task.thing varchar(60)},
 * {@code task.priority integer}, {@code task.createDate varchar(80)}), and every servlet -
 * {@code inside.AddTask}, {@code inside.Delete}, {@code inside.Edit}, {@code login.RequestLogin} and
 * {@code register.Register} - calls these methods, so the same value is validated the same way
 * everywhere and the rules cannot drift apart between servlets.
 */
public final class RequestValidator {

  /**
   * Account names are chosen at registration and stored in {@code accounts.name varchar(32)}. Only
   * printable identifier characters are accepted so a name can never carry control characters,
   * separators or markup into the database, the session or a page.
   */
  private static final Pattern ACCOUNT_NAME = Pattern.compile("[A-Za-z0-9._@-]{1,32}");

  /**
   * Passwords are stored in {@code accounts.password varchar(32)}. Any printable character is
   * allowed, control characters (CR/LF, NUL, ...) are not, and the length is bounded by the column
   * width.
   */
  private static final Pattern PASSWORD = Pattern.compile("[^\\p{Cntrl}]{1,32}");

  /**
   * Task text is free form but is stored in {@code task.thing varchar(60)}; control characters are
   * rejected so request data cannot forge log records or break the stored value.
   */
  private static final Pattern TASK_TEXT = Pattern.compile("[^\\p{Cntrl}]{1,60}");

  /**
   * Priority is stored in an integer column and the forms offer 1-10. Accept a positive integer,
   * bounded to nine digits so {@link Integer#parseInt(String)} can never overflow.
   */
  private static final Pattern PRIORITY = Pattern.compile("[1-9][0-9]{0,8}");

  /**
   * Task create dates are generated server-side from {@code Date#toString()} with blanks replaced by
   * underscores (e.g. "Mon_Sep_15_12:34:56_UTC_2026"), so only those characters are accepted.
   * Anything else (path separators, scheme/host characters, encoded escapes) is rejected.
   */
  private static final Pattern CREATE_DATE = Pattern.compile("[A-Za-z0-9 _:+-]{1,64}");

  /**
   * Widest value each parameter may occupy in the column it is stored in, counted in Java
   * {@code char}s (UTF-16 code units) exactly as the database counts them.
   *
   * <p>The quantifiers in the patterns above bound the number of <em>code points</em>, and a code
   * point outside the Basic Multilingual Plane (emoji, historic scripts, some CJK extensions) is two
   * chars wide. A value of 32 such code points therefore satisfies {@code {1,32}} while occupying 64
   * chars, which is wider than {@code varchar(32)}: the database then rejects (or truncates) it when
   * the servlet finally writes it. The insert failure is only logged - {@code register.Register}
   * still redirects to login.jsp - so an account or task would be silently lost. The stored width is
   * therefore checked here, at the trust boundary, in addition to the character allowlist.
   */
  private static final int ACCOUNT_NAME_MAX_CHARS = 32;

  /** See {@link #ACCOUNT_NAME_MAX_CHARS}: {@code accounts.password varchar(32)}. */
  private static final int PASSWORD_MAX_CHARS = 32;

  /** See {@link #ACCOUNT_NAME_MAX_CHARS}: {@code task.thing varchar(60)}. */
  private static final int TASK_TEXT_MAX_CHARS = 60;

  /** Digits only, so the pattern bound and the stored width are the same nine chars. */
  private static final int PRIORITY_MAX_CHARS = 9;

  /** Server-generated dates only; the pattern is already stricter than {@code varchar(80)}. */
  private static final int CREATE_DATE_MAX_CHARS = 64;

  /**
   * The login form renders "remember me" as a valueless checkbox (see login.jsp), so the browser
   * either omits the parameter or submits "on".
   */
  private static final String REMEMBER_ON = "on";

  private RequestValidator() {
    // Utility class.
  }

  /** Returns true when the value is an acceptable account name. */
  public static boolean isAccountName(String value) {
    return matches(ACCOUNT_NAME, value, ACCOUNT_NAME_MAX_CHARS);
  }

  /** Returns true when the value is an acceptable password. */
  public static boolean isPassword(String value) {
    return matches(PASSWORD, value, PASSWORD_MAX_CHARS);
  }

  /** Returns true when the value is acceptable task text. */
  public static boolean isTaskText(String value) {
    return matches(TASK_TEXT, value, TASK_TEXT_MAX_CHARS);
  }

  /** Returns true when the value is an acceptable task priority. */
  public static boolean isPriority(String value) {
    return matches(PRIORITY, value, PRIORITY_MAX_CHARS);
  }

  /** Returns true when the value is an acceptable server-generated task create date. */
  public static boolean isCreateDate(String value) {
    return matches(CREATE_DATE, value, CREATE_DATE_MAX_CHARS);
  }

  /**
   * Returns true when the submitted "remember me" value is one the login form can actually produce:
   * absent (checkbox unchecked) or the browser default "on". Any other value is rejected instead of
   * being treated as an opt-in.
   */
  public static boolean isRememberFlag(String value) {
    return value == null || REMEMBER_ON.equalsIgnoreCase(value);
  }

  /** Returns true when a validated "remember me" value means "keep me signed in". */
  public static boolean isRememberOptIn(String value) {
    return REMEMBER_ON.equalsIgnoreCase(value);
  }

  /**
   * Fail-closed check: the value must be present, must fit the column it is stored in (measured the
   * way the database measures it, in chars) and must consist only of allowlisted characters.
   */
  private static boolean matches(Pattern pattern, String value, int maxChars) {
    return value != null && value.length() <= maxChars && pattern.matcher(value).matches();
  }
}
