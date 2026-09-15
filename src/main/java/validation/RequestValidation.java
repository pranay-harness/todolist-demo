package validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation of the untrusted values that arrive as request parameters.
 *
 * <p>Every servlet passes a parameter through one of these methods at the point the value enters the
 * application, before it is used. A value that is missing, over-long or does not have the expected
 * format becomes {@code null} (or {@code false}) here, so the caller can fail closed instead of
 * letting an unchecked value reach a query, a redirect, session state or a rendered page.
 *
 * <p>The limits mirror the schema created in {@code register.Register#init()}:
 *
 * <pre>
 * accounts(name varchar(32), password varchar(255))
 * task(name varchar(32), thing varchar(60), priority integer, createDate varchar(80))
 * </pre>
 */
public final class RequestValidation {

  /** Width of the {@code task.thing} column. */
  public static final int MAX_TASK_TEXT_LENGTH = 60;

  /**
   * Longest password that is accepted. Only a fixed size one-way hash of a password is ever stored,
   * so this is not the width of the {@code password} column: it is a sanity bound that keeps an
   * unbounded value out of the deliberately expensive key derivation. A password is otherwise never
   * restricted, trimmed or truncated, so any character may be used.
   */
  public static final int MAX_PASSWORD_LENGTH = 128;

  /**
   * The shape an account name is allowed to have. Names live in a {@code varchar(32)} column, so
   * anything empty, longer than that or containing characters outside this set cannot be a name
   * this application issued and is never allowed to become session state or a query value.
   */
  private static final Pattern ACCOUNT_NAME = Pattern.compile("[A-Za-z0-9._@ -]{1,32}");

  /**
   * Allowlisted shape of the createDate token stored by AddTask (e.g. Mon_Sep_15_18:19:00_UTC_2026).
   * Rejects any scheme, "//" prefix, backslash, CR/LF or host, so a value used to build an
   * in-application link cannot point it elsewhere, and it always fits {@code varchar(80)}.
   */
  private static final Pattern CREATE_DATE = Pattern.compile("[A-Za-z0-9:_.+-]{1,64}");

  /**
   * Allowlisted priority: digits only, 1-9999, so the value is always a well-formed number that
   * fits the {@code task.priority} integer column and can be parsed without throwing.
   */
  private static final Pattern PRIORITY = Pattern.compile("[1-9][0-9]{0,3}");

  private RequestValidation() {
  }

  /**
   * Returns the account name, trimmed of surrounding whitespace, when it has the shape an account
   * name is stored with, otherwise null.
   */
  public static String accountName(String value) {
    if (value == null) {
      return null;
    }
    String accountName = value.trim();
    if (!ACCOUNT_NAME.matcher(accountName).matches()) {
      return null;
    }
    return accountName;
  }

  /**
   * Returns the supplied password unchanged when one was supplied and it is not absurdly long,
   * otherwise null. The value is never trimmed, truncated or restricted to a character set.
   */
  public static String password(String value) {
    if (value == null || value.isEmpty() || value.length() > MAX_PASSWORD_LENGTH) {
      return null;
    }
    return value;
  }

  /**
   * Returns the task text when it is non-blank and fits the {@code task.thing} column, otherwise
   * null. The text itself is preserved as typed.
   */
  public static String taskText(String value) {
    if (value == null || value.trim().isEmpty() || value.length() > MAX_TASK_TEXT_LENGTH) {
      return null;
    }
    return value;
  }

  /**
   * Returns the priority when it is a small positive integer of the allowlisted shape, otherwise
   * null. A returned value is always safe to parse with {@link Integer#parseInt(String)}.
   */
  public static String priority(String value) {
    if (value == null || !PRIORITY.matcher(value).matches()) {
      return null;
    }
    return value;
  }

  /**
   * Returns the createDate task key when it has the allowlisted token shape, otherwise null.
   */
  public static String createDate(String value) {
    if (value == null || !CREATE_DATE.matcher(value).matches()) {
      return null;
    }
    return value;
  }

  /**
   * Returns true only for the values a checkbox submits when it is ticked. Anything else, including
   * a missing or unrecognised value, is treated as not ticked so the safer default applies.
   */
  public static boolean isChecked(String value) {
    if (value == null) {
      return false;
    }
    String flag = value.trim().toLowerCase(Locale.ROOT);
    return flag.equals("on") || flag.equals("true") || flag.equals("yes") || flag.equals("1");
  }
}
