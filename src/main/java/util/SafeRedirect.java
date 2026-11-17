package util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects only to fixed, allowlisted in-app paths. Absolute, scheme-relative and
 * CRLF-bearing targets are never accepted; user-supplied values may only be added as
 * URL-encoded query parameter values.
 */
public final class SafeRedirect {
  private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
      "/login.jsp",
      "/loginFault.jsp",
      "/wrongRegister.jsp",
      "/userExists.jsp",
      "/inside/display",
      "/inside/showEditTask.jsp")));

  /** Empty (root) context, or one or more path segments such as "/app". */
  private static final Pattern CONTEXT_PATH = Pattern.compile("^(/[A-Za-z0-9._~-]+)*$");

  private SafeRedirect() {
  }

  /** Redirect to {@code contextPath + target}, where target must be an allowlisted app path. */
  public static void toAppPath(HttpServletRequest request, HttpServletResponse response, String target) throws IOException {
    String contextPath = request.getContextPath();
    if (contextPath == null) {
      contextPath = "";
    }
    if (!ALLOWED_PATHS.contains(target) || !CONTEXT_PATH.matcher(contextPath).matches()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    response.sendRedirect(contextPath + target);
  }

  /** Redirect to an allowlisted app path with one URL-encoded query parameter. */
  public static void toAppPathWithParam(HttpServletResponse response, String target, String paramName, String paramValue)
      throws IOException {
    if (!ALLOWED_PATHS.contains(target)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    response.sendRedirect(target + "?" + encode(paramName) + "=" + encode(String.valueOf(paramValue)));
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }
}
