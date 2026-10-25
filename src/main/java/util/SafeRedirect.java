package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Builds redirect targets that can only ever point back into this web application.
 *
 * <p>Every destination is looked up in a fixed allowlist of internal, context relative paths, so no request supplied
 * value can become the target of an HTTP redirect (CWE-601 open redirect). Request data may only travel as a URL
 * encoded query parameter of an allowlisted path, which also neutralises protocol relative ("//host"), backslash and
 * CR/LF tricks.</p>
 */
public final class SafeRedirect {

  /** The only context relative paths this application is allowed to redirect to. */
  private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
      "/index.jsp", "/login", "/login.jsp", "/loginFault.jsp", "/loginFirst.jsp", "/register", "/userExists.jsp",
      "/wrongRegister.jsp", "/inside/display", "/inside/showEditTask.jsp")));

  /** Fallback used whenever a caller asks for a path that is not on the allowlist. */
  private static final String DEFAULT_PATH = "/index.jsp";

  /** Highest character code that is still a control character (DEL). */
  private static final char DELETE_CHARACTER = 127;

  private SafeRedirect() {
  }

  /**
   * Returns an application local URL for the given allowlisted path.
   */
  public static String internalUrl(HttpServletRequest request, String path) {
    return contextPathOf(request) + allowedPath(path);
  }

  /**
   * Returns an application local URL for the given allowlisted path, carrying a single URL encoded query parameter.
   */
  public static String internalUrl(HttpServletRequest request, String path, String parameterName, String parameterValue) {
    String url = internalUrl(request, path);
    String query = encodedParameter(parameterName, parameterValue);
    return query.isEmpty() ? url : url + "?" + query;
  }

  private static String allowedPath(String path) {
    return ALLOWED_PATHS.contains(path) ? path : DEFAULT_PATH;
  }

  /**
   * Returns the deployment context path, or an empty string when it is missing or is not a plain path with a single
   * leading slash (absolute URL, "//host", backslash or control character forms are all rejected).
   */
  private static String contextPathOf(HttpServletRequest request) {
    String contextPath = request == null ? null : request.getContextPath();
    if (contextPath == null || contextPath.isEmpty()) {
      return "";
    }
    if (contextPath.charAt(0) != '/' || contextPath.startsWith("//")) {
      return "";
    }
    for (int i = 0; i < contextPath.length(); i++) {
      char character = contextPath.charAt(i);
      if (character == '\\' || character == ':' || character <= ' ' || character == DELETE_CHARACTER) {
        return "";
      }
    }
    return contextPath;
  }

  private static String encodedParameter(String name, String value) {
    if (name == null || name.isEmpty() || value == null) {
      return "";
    }
    try {
      return URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }
}
