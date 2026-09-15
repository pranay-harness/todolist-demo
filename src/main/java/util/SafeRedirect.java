package util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Central guard for HTTP redirects (CWE-601, URL Redirection to Untrusted Site).
 *
 * <p>Servlets must never hand request derived data to {@link HttpServletResponse#sendRedirect(String)}.
 * Every destination this application redirects to is declared here as a constant; callers pick one of
 * those constants and this class resolves the canonical, application relative location from its own
 * allowlist. A value that is not on the allowlist can never reach the response, so absolute URLs
 * ({@code http://evil.example}), scheme relative targets ({@code //evil.example}), backslash variants
 * ({@code /\evil.example}) and traversal escapes ({@code /../..}) are all rejected by construction.</p>
 *
 * <p>Optional query string values are URL encoded, which keeps user supplied data inside the query of
 * an allowlisted path instead of letting it change the host or the path.</p>
 */
public final class SafeRedirect {

  /** Login form. */
  public static final String LOGIN = "/login.jsp";

  /** Page shown after a failed login attempt. */
  public static final String LOGIN_FAULT = "/loginFault.jsp";

  /** Page shown when a session is required. */
  public static final String LOGIN_FIRST = "/loginFirst.jsp";

  /** Page shown after an invalid registration attempt. */
  public static final String WRONG_REGISTER = "/wrongRegister.jsp";

  /** Page shown when the requested account name is taken. */
  public static final String USER_EXISTS = "/userExists.jsp";

  /** Task list controller. */
  public static final String INSIDE_DISPLAY = "/inside/display";

  /** Single task edit page. */
  public static final String INSIDE_EDIT_TASK = "/inside/showEditTask.jsp";

  /** Destination used when a caller asks for something that is not allowlisted. */
  private static final String FALLBACK_TARGET = LOGIN;

  /** The only locations this application is allowed to redirect to. */
  private static final Set<String> ALLOWED_TARGETS = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
      LOGIN, LOGIN_FAULT, LOGIN_FIRST, WRONG_REGISTER, USER_EXISTS, INSIDE_DISPLAY, INSIDE_EDIT_TASK)));

  /** An empty context path, or one or more plain path segments - nothing that could switch host. */
  private static final Pattern SAFE_CONTEXT_PATH = Pattern.compile("(/[A-Za-z0-9._~-]+)*");

  /** Query parameter names are restricted to simple identifiers. */
  private static final Pattern SAFE_PARAMETER_NAME = Pattern.compile("[A-Za-z0-9_-]+");

  private SafeRedirect() {
    // utility class, not instantiable
  }

  /**
   * Tells whether the given location is one of the declared application destinations.
   */
  public static boolean isAllowed(String target) {
    return target != null && ALLOWED_TARGETS.contains(target);
  }

  /**
   * Resolves an allowlisted, application relative location. The returned value is always one of the
   * constants declared in this class: caller supplied text is compared against the allowlist and then
   * discarded, so it cannot contribute to the redirect location. Unknown destinations fall back to the
   * login page rather than being followed.
   */
  public static String resolve(String target) {
    for (String allowed : ALLOWED_TARGETS) {
      if (allowed.equals(target)) {
        return allowed;
      }
    }
    return FALLBACK_TARGET;
  }

  /**
   * Builds the full redirect location for an allowlisted destination inside this web application.
   */
  public static String location(HttpServletRequest request, String target) {
    return contextPath(request) + resolve(target);
  }

  /**
   * Builds the full redirect location for an allowlisted destination, carrying a single URL encoded
   * query parameter. The parameter is dropped when its name is not a simple identifier or its value is
   * missing.
   */
  public static String location(HttpServletRequest request, String target, String parameterName, String parameterValue) {
    String location = location(request, target);
    if (parameterValue == null || parameterName == null || !SAFE_PARAMETER_NAME.matcher(parameterName).matches()) {
      return location;
    }
    return location + "?" + parameterName + "=" + encode(parameterValue);
  }

  /**
   * Redirects to an allowlisted destination inside this web application.
   */
  public static void send(HttpServletRequest request, HttpServletResponse response, String target) throws IOException {
    response.sendRedirect(location(request, target));
  }

  /**
   * Redirects to an allowlisted destination inside this web application, carrying a single URL encoded
   * query parameter.
   */
  public static void send(HttpServletRequest request, HttpServletResponse response, String target, String parameterName,
      String parameterValue) throws IOException {
    response.sendRedirect(location(request, target, parameterName, parameterValue));
  }

  /**
   * Returns the context path of the current request when it is a plain path, otherwise the empty
   * string. Guards against containers that echo an attacker influenced request URI back to us.
   */
  private static String contextPath(HttpServletRequest request) {
    if (request == null) {
      return "";
    }
    String contextPath = request.getContextPath();
    if (contextPath == null || !SAFE_CONTEXT_PATH.matcher(contextPath).matches()) {
      return "";
    }
    return contextPath;
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is required to be present on every JVM; drop the value rather than emit it raw.
      return "";
    }
  }
}
