package util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility for performing safe HTTP redirects that prevent open redirect vulnerabilities
 * (CWE-601).
 *
 * <p>All redirect targets must be application-relative paths that:
 * <ul>
 *   <li>start with {@code "/"}</li>
 *   <li>do <em>not</em> start with {@code "//"} (protocol-relative URL)</li>
 *   <li>do <em>not</em> contain {@code "://"} (absolute URL with scheme)</li>
 * </ul>
 * Any path that does not meet these requirements is rejected with an HTTP 400 response.
 */
public final class RedirectUtil {

    private RedirectUtil() {
        // utility class — do not instantiate
    }

    /**
     * Redirects the client to an application-relative path, prepending the servlet context path.
     *
     * <p>The {@code appRelativePath} must start with {@code "/"}, must not start with
     * {@code "//"}, and must not contain {@code "://"}.  Paths that fail validation are
     * rejected with {@code HTTP 400}, preventing open redirect attacks.
     *
     * @param request         the current HTTP request (used to obtain the context path)
     * @param response        the current HTTP response
     * @param appRelativePath a validated, application-relative path (e.g. {@code "/login.jsp"})
     * @throws IOException if an I/O error occurs while sending the redirect or error response
     */
    public static void safeRedirect(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String appRelativePath) throws IOException {
        if (!isSafeRelativePath(appRelativePath)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect target");
            return;
        }
        response.sendRedirect(request.getContextPath() + appRelativePath);
    }

    /**
     * Returns {@code true} if {@code path} is a safe, application-relative path.
     *
     * <p>A safe path must start with {@code "/"}, must not start with {@code "//"},
     * and must not contain {@code "://"}.
     *
     * @param path the path to validate
     * @return {@code true} if the path is safe; {@code false} otherwise
     */
    public static boolean isSafeRelativePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // Must start with "/" but not "//" (protocol-relative URL)
        if (!path.startsWith("/") || path.startsWith("//")) {
            return false;
        }
        // Must not contain a URL scheme (e.g. "http://", "https://", "javascript://")
        if (path.contains("://")) {
            return false;
        }
        return true;
    }

    /**
     * URL-encodes a single query-parameter value using UTF-8.
     *
     * <p>Use this method when a user-supplied value must appear as a query parameter in a
     * redirect URL, to prevent CRLF injection and parameter manipulation.
     *
     * @param value the raw parameter value (may be {@code null})
     * @return the URL-encoded value, or an empty string if {@code value} is {@code null}
     */
    public static String encodeParam(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is mandated by the Java specification; this branch is unreachable
            throw new IllegalStateException("UTF-8 encoding not supported", e);
        }
    }
}
