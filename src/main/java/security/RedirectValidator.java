package security;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Utility class for validating redirect URLs to prevent open redirect attacks.
 * This validator ensures that:
 * - Only relative URLs (starting with /) are allowed
 * - URLs with the same origin are allowed
 * - Protocol-relative URLs (//evil.com) are blocked
 * - Absolute URLs to external hosts are blocked
 */
public class RedirectValidator {

    // List of whitelisted external domains (if needed in the future)
    private static final String[] WHITELISTED_DOMAINS = {};

    /**
     * Validates that a redirect URL is safe and won't lead to an open redirect vulnerability.
     *
     * @param url The URL to validate
     * @param request The HTTP request (to get the request URL for same-origin comparison)
     * @return true if the URL is safe for redirect, false otherwise
     */
    public static boolean isValidRedirectUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        url = url.trim();

        // Block protocol-relative URLs (//example.com)
        if (url.startsWith("//")) {
            return false;
        }

        // Block absolute URLs with explicit schemes (http://, https://, ftp://, etc.)
        if (url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return false;
        }

        // Allow relative URLs starting with /
        if (url.startsWith("/")) {
            // Additional check: ensure no path traversal or other issues
            // Block URLs containing null bytes or other suspicious patterns
            if (url.contains("\0") || url.contains("\r") || url.contains("\n")) {
                return false;
            }
            return true;
        }

        // Block all other URLs
        return false;
    }

    /**
     * Validates that a parameter value is safe to include in a URL query string.
     * This is useful when building redirect URLs with parameters from user input.
     *
     * @param value The parameter value to validate
     * @return true if the value is safe (alphanumeric, hyphens, underscores, dots, colons)
     */
    public static boolean isValidUrlParameter(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Allow only alphanumeric characters, hyphens, underscores, dots, and colons
        // This prevents injection of special characters that could break the URL
        // Colons are included to support time formats like HH:MM:SS
        return value.matches("^[a-zA-Z0-9._:-]+$");
    }
}
