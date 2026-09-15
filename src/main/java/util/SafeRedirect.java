package util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

/**
 * Allow-list based redirect helper (CWE-601, Open Redirect).
 *
 * <p>Every application redirect goes through this class, so the destination can only ever be one of the fixed
 * in-application paths declared below. The target is never taken from the HTTP request: callers pass one of the
 * constants of this class and the value handed to {@link HttpServletResponse#sendRedirect(String)} is the matching
 * literal owned here. That makes absolute URLs ({@code http://evil.example}), protocol relative URLs
 * ({@code //evil.example}), backslash variants ({@code /\evil.example}) and path traversal impossible.
 *
 * <p>Query string values may come from user input, so they are accepted only when they match a strict character
 * allow-list and they are URL encoded before being appended. They therefore cannot introduce a new path, host or
 * scheme.
 */
public final class SafeRedirect {

  /** Public landing page. */
  public static final String INDEX = "/index.jsp";

  /** Login form. */
  public static final String LOGIN_PAGE = "/login.jsp";

  /** Shown when the submitted credentials are rejected. */
  public static final String LOGIN_FAULT_PAGE = "/loginFault.jsp";

  /** Shown when a registration form was filled in incorrectly. */
  public static final String WRONG_REGISTER_PAGE = "/wrongRegister.jsp";

  /** Shown when the requested account name is already taken. */
  public static final String USER_EXISTS_PAGE = "/userExists.jsp";

  /** The authenticated to-do list. */
  public static final String DISPLAY = "/inside/display";

  /** The edit form of a single task. */
  public static final String SHOW_EDIT_TASK_PAGE = "/inside/showEditTask.jsp";

  /** Every destination this application is allowed to redirect to. */
  private static final String[] ALLOWED_TARGETS = {
      INDEX, LOGIN_PAGE, LOGIN_FAULT_PAGE, WRONG_REGISTER_PAGE, USER_EXISTS_PAGE, DISPLAY, SHOW_EDIT_TASK_PAGE
  };

  /** Fail closed destination, used when a caller asks for a target that is not on the allow-list. */
  private static final String DEFAULT_TARGET = INDEX;

  /**
   * Characters accepted in a redirect query string value. Task keys are created dates such as
   * "Mon_Sep_15_10:11:12_UTC_2025", so no slash, backslash, dot segment or whitespace is needed.
   */
  private static final Pattern SAFE_PARAMETER_VALUE = Pattern.compile("[A-Za-z0-9_:.+-]{1,64}");

  private SafeRedirect() {
    // Utility class.
  }

  /**
   * Redirects to an allow-listed application path.
   *
   * @param context the servlet context, used for the deployment context path (never request data)
   * @param response the response to redirect
   * @param target one of the target constants of this class
   */
  public static void to(ServletContext context, HttpServletResponse response, String target) throws IOException {
    response.sendRedirect(response.encodeRedirectURL(contextPath(context) + allowListed(target)));
  }

  /**
   * Redirects to an allow-listed application path, carrying a single query string parameter.
   *
   * <p>The parameter is dropped when its value is missing or does not match {@link #SAFE_PARAMETER_VALUE}; the
   * redirect itself still happens, to the bare allow-listed path.
   *
   * @param context the servlet context, used for the deployment context path (never request data)
   * @param response the response to redirect
   * @param target one of the target constants of this class
   * @param parameterName the query string parameter name
   * @param parameterValue the query string parameter value, possibly user supplied
   */
  public static void to(ServletContext context, HttpServletResponse response, String target, String parameterName,
      String parameterValue) throws IOException {
    String url = contextPath(context) + allowListed(target);
    if (parameterName != null && parameterValue != null && SAFE_PARAMETER_VALUE.matcher(parameterValue).matches()) {
      url = url + "?" + encode(parameterName) + "=" + encode(parameterValue);
    }
    response.sendRedirect(response.encodeRedirectURL(url));
  }

  /**
   * Returns the allow-listed literal equal to {@code target}, or {@link #DEFAULT_TARGET} when the target is unknown.
   */
  private static String allowListed(String target) {
    for (String allowed : ALLOWED_TARGETS) {
      if (allowed.equals(target)) {
        return allowed;
      }
    }
    return DEFAULT_TARGET;
  }

  private static String contextPath(ServletContext context) {
    if (context == null) {
      return "";
    }
    String path = context.getContextPath();
    if (path == null || "/".equals(path)) {
      return "";
    }
    return path;
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is required to be present on every JVM; fall back to the empty value rather than to raw input.
      return "";
    }
  }
}
