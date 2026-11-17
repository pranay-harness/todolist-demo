# Security

Security rules for this repository. Marked blocks are maintained by Harness remediation. Text outside those blocks is left as written.


<!-- harness-remediation:path-containment -->
### path-containment — Contain user-supplied file names under a root

Upload and download paths were built with root.resolve(untrustedFileName) and no containment check. UnrestrictedFileUpload.resolveWithinRoot (absolute + normalize + startsWith(root)) now guards both genericFileUploadUtility and PreflightController.fetchFile. Route any new file-name-to-Path code through that helper.
<!-- /harness-remediation -->

<!-- harness-remediation:bound-sql-parameters -->
### bound-sql-parameters — Bind user input in JdbcTemplate queries

The SQL injection controllers and AuthLoginService.authenticateLevel1SQLi concatenated request params into SQL. They now use bound parameters (prepareStatement with setString, or varargs args), matching the secure levels. Always pass user input as bound args, never concatenate.
<!-- /harness-remediation -->

<!-- harness-remediation:argv-command-execution -->
### argv-command-execution — Run commands with argv and an allowlist

CommandInjection.getResponseFromPingCommand built a shell string with the user ipAddress. It now runs ping through ProcessBuilder with an argv array, after the value matches a strict host/IP allowlist (SAFE_HOST_PATTERN, no leading dash). Pass user data as separate argv entries and validate it first.
<!-- /harness-remediation -->

<!-- harness-remediation:ssrf-destination-guard -->
### ssrf-destination-guard — Guard outbound fetches of user URLs

SSRFVulnerability and UrlParamBasedRFI fetched user URLs with only string checks. SsrfUrlGuard.isSafeToFetch now allows http/https only, requires every resolved IP to be public (rejecting loopback, link-local, metadata and private ranges), and redirects are not followed. Reuse SsrfUrlGuard for any new outbound fetch.
<!-- /harness-remediation -->

<!-- harness-remediation:html-escape-at-sink -->
### html-escape-at-sink — Escape untrusted values where HTML is built

Raw request params were concatenated into HTML in the XSS and cache-poisoning controllers. They now pass through the existing StringEscapeUtils.escapeHtml4 at the String.format site, the img src attribute is quoted, and the forwarded host uses an allowlist regex. Escape at the output sink instead of relying on keyword blocklists.
<!-- /harness-remediation -->

<!-- harness-remediation:safe-redirect-allowlist -->
### safe-redirect-allowlist — Redirect only to allowlisted app paths

Servlets built redirect targets by concatenating request.getContextPath() or request params into sendRedirect. util.SafeRedirect now redirects only to allowlisted fixed app paths and URL-encodes user values as query parameters. Route new redirects through SafeRedirect and add the path to its allowlist.
<!-- /harness-remediation -->

<!-- harness-remediation:secure-random -->
### secure-random — Use SecureRandom for security-relevant values

UnrestrictedFileUpload used a static java.util.Random for upload filename prefixes and OTPService.requestOTPLevel2 seeded Random with the clock. Both now use java.security.SecureRandom with the same formats. Use SecureRandom for any identifier or code that has security value.
<!-- /harness-remediation -->

<!-- harness-remediation:generic-error-responses -->
### generic-error-responses — Keep exception text out of responses

SQL and LDAP exception messages were concatenated into HTTP response bodies. The detail is now logged server-side with CR/LF stripped, and the response carries a fixed generic message. Do not put e.getMessage() into response beans.
<!-- /harness-remediation -->

<!-- harness-remediation:log-input-sanitising -->
### log-input-sanitising — Sanitise untrusted values before logging

Request-supplied username and URL were passed straight to Log4j. They are now run through replaceAll("\\p{Cntrl}", "_") before logging, and the password is no longer logged. Sanitise any untrusted value before it reaches LOGGER.
<!-- /harness-remediation -->

<!-- harness-remediation:explicit-content-type -->
### explicit-content-type — Set non-HTML content types for plain responses

Plain res.send(string) in src/main/test1.ts defaulted to text/html, and php-app/public/index.php echoed json_encode without hex flags. The 401 responses now use .type('text/plain') and json_encode uses JSON_HEX_TAG|AMP|APOS|QUOT. Prefer explicit non-HTML content types for plain-text bodies.
<!-- /harness-remediation -->

<!-- harness-remediation:httponly-cookies -->
### httponly-cookies — Set HttpOnly on every cookie

IDORLoginController built Cookie objects without HttpOnly. The existing setTokenCookie pattern (setHttpOnly(true) plus setSecure(request.isSecure())) now applies to every cookie there. These cookies are only read server-side via @CookieValue, so the UI is unaffected.
<!-- /harness-remediation -->

<!-- harness-remediation:no-hardcoded-secrets -->
### no-hardcoded-secrets — Load secrets from the environment and fail closed

CryptographicFailuresSeeder, JWTAlgorithmKMS and test1.ts held hardcoded credentials. They now read CRYPTO_FAILURES_LEVEL10_SECRET, JWT_KEY_STORE_PASSWORD and CHALLENGE_*_PASSWORD at runtime and fail closed when unset, with timingSafeEqual for the TS compare. The old values remain in git history and must be rotated. Supply new ones via env or a secrets manager.
<!-- /harness-remediation -->
