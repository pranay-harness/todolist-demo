package util;

/**
 * Utility class for safe HTTP redirect URL validation.
 * Prevents open redirect vulnerabilities (CWE-601) by ensuring redirect
 * targets are relative paths on the same server.
 */
public final class SafeRedirect {

    private SafeRedirect() {
        // Utility class — not instantiable
    }

    /**
     * Returns {@code true} if {@code url} is a safe relative path.
     * <p>
     * A safe relative path:
     * <ul>
     *   <li>starts with {@code /}</li>
     *   <li>does NOT start with {@code //} (protocol-relative URLs can point to
     *       external hosts, e.g. {@code //evil.com/path})</li>
     *   <li>does NOT contain {@code ://} (absolute URL)</li>
     * </ul>
     *
     * @param url the URL to validate
     * @return {@code true} if the URL is a safe relative redirect target
     */
    public static boolean isSafeRelativeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        // Must start with '/' to be a relative server-rooted path
        if (!url.startsWith("/")) {
            return false;
        }
        // '//' is a protocol-relative URL and can redirect to an external host
        if (url.startsWith("//")) {
            return false;
        }
        // Reject any absolute URL that slipped past the leading-slash check
        if (url.contains("://")) {
            return false;
        }
        return true;
    }

    /**
     * Returns {@code url} if it is a safe relative path; otherwise returns
     * {@code fallback}. Use this to enforce a safe redirect target when the
     * URL is derived from non-constant values (e.g. {@code getContextPath()}).
     *
     * @param url      the URL to validate
     * @param fallback the safe fallback URL to use when {@code url} fails validation
     * @return the validated URL or the fallback
     */
    public static String getSafeUrl(String url, String fallback) {
        return isSafeRelativeUrl(url) ? url : fallback;
    }
}
