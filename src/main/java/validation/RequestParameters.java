package validation;

import java.util.regex.Pattern;

/**
 * Allowlist validation for untrusted servlet request parameters.
 *
 * <p>Every value read with HttpServletRequest#getParameter is attacker controlled, so each servlet
 * validates it here at the trust boundary before it reaches the database, a redirect target or the
 * application log. The shapes below are deliberately strict: they mirror the column widths declared
 * for the accounts and task tables and they reject control characters, so an untrusted value can
 * neither overflow a column nor forge extra log records.
 */
public final class RequestParameters {

  /** accounts.name / task.name is varchar(32); user names are plain identifiers. */
  private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._@-]{1,32}");

  /** accounts.password is varchar(32); any printable character is allowed inside that bound. */
  private static final Pattern PASSWORD = Pattern.compile("[^\\p{Cntrl}]{1,32}");

  /** task.thing is varchar(60); control characters (CR/LF/NUL) are rejected. */
  private static final Pattern TASK = Pattern.compile("[^\\p{Cntrl}]{1,60}");

  /**
   * task.priority is an integer column and the "Priority(1-10)" inputs only ever submit a positive
   * number without leading zeros. The length bound keeps the value inside the range of an int.
   */
  private static final Pattern PRIORITY = Pattern.compile("[1-9][0-9]{0,8}");

  /**
   * task.createDate is varchar(80) and AddTask stores Date.toString() with spaces replaced by '_'
   * (e.g. Mon_Sep_15_12:00:00_UTC_2026). Anything else is rejected.
   */
  private static final Pattern DATE = Pattern.compile("[A-Za-z0-9_:.+-]{1,64}");

  /** Shape of a checkbox value such as the login form's "remember" flag (submitted as "on"). */
  private static final Pattern FLAG = Pattern.compile("[A-Za-z0-9_-]{1,16}");

  private RequestParameters() {
  }

  /** Returns true when the value is a valid account name. */
  public static boolean isValidName(String value) {
    return matches(value, NAME);
  }

  /** Returns true when the value is a valid password. */
  public static boolean isValidPassword(String value) {
    return matches(value, PASSWORD);
  }

  /** Returns true when the value is a valid task description. */
  public static boolean isValidTask(String value) {
    return matches(value, TASK);
  }

  /** Returns true when the value is a valid task priority. */
  public static boolean isValidPriority(String value) {
    return matches(value, PRIORITY);
  }

  /** Returns true when the value is a valid task createDate. */
  public static boolean isValidDate(String value) {
    return matches(value, DATE);
  }

  /**
   * Returns true when an optional checkbox parameter is either absent (unchecked, which the callers
   * already treat as "off") or has the expected flag shape.
   */
  public static boolean isAbsentOrValidFlag(String value) {
    return value == null || matches(value, FLAG);
  }

  private static boolean matches(String value, Pattern shape) {
    return value != null && shape.matcher(value).matches();
  }
}
