package validation;

import java.util.regex.Pattern;

/**
 * Allowlists for the request parameters the servlets accept.
 *
 * <p>Every check is fail closed: {@code null}, empty and out-of-range values are rejected, so an
 * unvalidated request parameter never reaches the database, a redirect or the session. The bounds
 * mirror the schema created in {@code register.Register#init()} ({@code accounts.name varchar(32)},
 * {@code accounts.password varchar(32)}, {@code task.thing varchar(60)},
 * {@code task.priority integer}, {@code task.createDate varchar(80)}) and the rules already applied
 * in {@code inside.Edit}, so the same value is validated the same way in every servlet.
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
   * rejected so request data cannot forge log records or break the stored value. Same rule as
   * {@code inside.Edit}.
   */
  private static final Pattern TASK_TEXT = Pattern.compile("[^\\p{Cntrl}]{1,60}");

  /**
   * Priority is stored in an integer column and the forms offer 1-10. Accept a positive integer,
   * bounded to nine digits so {@link Integer#parseInt(String)} can never overflow. Same rule as
   * {@code inside.Edit}.
   */
  private static final Pattern PRIORITY = Pattern.compile("[1-9][0-9]{0,8}");

  /**
   * Task create dates are generated server-side from {@code Date#toString()} with blanks replaced by
   * underscores (e.g. "Mon_Sep_15_12:34:56_UTC_2026"), so only those characters are accepted.
   * Anything else (path separators, scheme/host characters, encoded escapes) is rejected. Same rule
   * as {@code inside.Edit}.
   */
  private static final Pattern CREATE_DATE = Pattern.compile("[A-Za-z0-9 _:+-]{1,64}");

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
    return matches(ACCOUNT_NAME, value);
  }

  /** Returns true when the value is an acceptable password. */
  public static boolean isPassword(String value) {
    return matches(PASSWORD, value);
  }

  /** Returns true when the value is acceptable task text. */
  public static boolean isTaskText(String value) {
    return matches(TASK_TEXT, value);
  }

  /** Returns true when the value is an acceptable task priority. */
  public static boolean isPriority(String value) {
    return matches(PRIORITY, value);
  }

  /** Returns true when the value is an acceptable server-generated task create date. */
  public static boolean isCreateDate(String value) {
    return matches(CREATE_DATE, value);
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

  private static boolean matches(Pattern pattern, String value) {
    return value != null && pattern.matcher(value).matches();
  }
}
