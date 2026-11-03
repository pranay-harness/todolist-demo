package util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility for performing HTTP redirects only to known, allowlisted internal paths.
 * Prevents open redirect vulnerabilities by validating both the destination path
 * and any user-supplied query parameters before issuing the redirect.
 */
public final class SafeRedirect {

  /** Allowlist of internal paths that may be used as redirect destinations. */
  private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "/login.jsp",
          "/loginFault.jsp",
          "/wrongRegister.jsp",
          "/userExists.jsp",
          "/inside/display",
          "/inside/showEditTask.jsp"
      ))
  );

  private SafeRedirect() {}

  /**
   * Redirects to a known, allowlisted internal path.
   * If {@code path} is not in the allowlist the request is redirected to
   * {@code /login.jsp} instead, preventing any open redirect.
   *
   * @param request  the current HTTP request (used to obtain the context path)
   * @param response the current HTTP response
   * @param path     the internal path to redirect to (must be in ALLOWED_PATHS)
   * @throws IOException if the redirect fails
   */
  public static void to(HttpServletRequest request, HttpServletResponse response, String path)
      throws IOException {
    if (!ALLOWED_PATHS.contains(path)) {
      path = "/login.jsp";
    }
    response.sendRedirect(validatedContextPath(request) + path);
  }

  /**
   * Redirects to an allowlisted internal path with a single URL-encoded query parameter.
   * The parameter value is URL-encoded to prevent header-injection via newline characters.
   * If {@code path} is not in the allowlist the request is redirected to
   * {@code /login.jsp} instead.
   *
   * @param request    the current HTTP request
   * @param response   the current HTTP response
   * @param path       the internal path to redirect to (must be in ALLOWED_PATHS)
   * @param paramName  the query parameter name (must be a compile-time constant)
   * @param paramValue the query parameter value (URL-encoded before use)
   * @throws IOException if the redirect fails
   */
  public static void toWithParam(HttpServletRequest request, HttpServletResponse response,
      String path, String paramName, String paramValue) throws IOException {
    if (!ALLOWED_PATHS.contains(path)) {
      path = "/login.jsp";
    }
    String encodedValue = URLEncoder.encode(paramValue != null ? paramValue : "", "UTF-8");
    response.sendRedirect(validatedContextPath(request) + path + "?" + paramName + "=" + encodedValue);
  }

  /**
   * Returns the servlet context path only when it is a safe relative value
   * (no URI scheme, no host — i.e. cannot redirect the browser to an external origin).
   * Returns an empty string when the context path looks unsafe.
   */
  private static String validatedContextPath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    if (contextPath == null
        || contextPath.contains("://")
        || contextPath.startsWith("//")) {
      return "";
    }
    return contextPath;
  }
}
