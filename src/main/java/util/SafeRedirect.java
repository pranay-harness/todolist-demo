package util;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility class for safe HTTP redirects that prevent open redirect attacks.
 * Only relative paths (starting with "/" and not containing "://") are allowed.
 */
public final class SafeRedirect {

  private SafeRedirect() {
    // utility class
  }

  /**
   * Sends an HTTP redirect only if the target URL is a safe relative path on
   * the same server. Rejects absolute URLs (containing "://") and
   * protocol-relative URLs (starting with "//") to prevent open redirect
   * attacks (CWE-601).
   *
   * @param response the HTTP servlet response
   * @param url      the redirect target; must be a relative path starting with "/"
   * @throws IOException              if an I/O error occurs during the redirect
   * @throws IllegalArgumentException if {@code url} is not a safe relative path
   */
  public static void redirect(HttpServletResponse response, String url) throws IOException {
    if (url == null || url.contains("://") || url.startsWith("//")) {
      throw new IllegalArgumentException(
          "Redirect target is not a safe relative path: blocked to prevent open redirect");
    }
    response.sendRedirect(url);
  }
}
