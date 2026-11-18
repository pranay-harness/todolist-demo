Instructions to the Remediation agent:

Do not upgrade the dependency httpclient even if there are vulnerabilities as this is used for testing purpose



<!-- harness-remediation:sec-path-traversal -->
### sec-path-traversal — Canonicalize paths before file I/O

Resolve, normalize, and check that the absolute path starts with the allowed root prefix before opening files; apply this in both upload utilities and download controllers.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-sql-parameterize -->
### sec-sql-parameterize — Use parameterized SQL queries

Replace all string concatenation into SQL with PreparedStatement bind parameters (?); never concatenate user-controlled input into query strings.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-command-array -->
### sec-command-array — Use ProcessBuilder argument arrays to prevent command injection

Avoid shell wrappers (sh -c); pass each command argument separately to ProcessBuilder and validate inputs with an allowlist regex before execution.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-ssrf-allowlist -->
### sec-ssrf-allowlist — Validate URLs to prevent SSRF and open redirect

For SSRF, reject non-http/https schemes and private/loopback IP addresses via InetAddress; for open redirects, enforce allowlisted destinations or relative-path-only targets.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-html-encode -->
### sec-html-encode — HTML-encode attacker data before embedding in responses

Wrap all user-controlled values with StringEscapeUtils.escapeHtml4() at the HTML construction point to prevent reflected XSS.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-secure-random -->
### sec-secure-random — Use SecureRandom for security-sensitive values

Replace java.util.Random with java.security.SecureRandom (or UUID.randomUUID()) for tokens, session IDs, OTPs, and file identifiers.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-bcrypt-password -->
### sec-bcrypt-password — Use BCrypt instead of MD5/SHA-1 for passwords

Replace weak hash algorithms (MD5, SHA-1) with BCryptPasswordEncoder.matches(); restrict hash helper methods to allow only SHA-256/SHA-512.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-env-secrets -->
### sec-env-secrets — Load secrets from environment variables

Replace hardcoded passwords and keys with System.getenv() lookups; rotate any previously committed credential values.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-exception-info -->
### sec-exception-info — Log exceptions internally, return generic messages externally

Log e.getMessage() server-side via LOGGER and return a generic error message to HTTP clients to prevent sensitive data exposure.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-log-forging -->
### sec-log-forging — Sanitize user input before logging

Call .replaceAll("[\r\n]", "_") on user-supplied strings before passing to LOGGER to prevent log forging.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-httponly-cookie -->
### sec-httponly-cookie — Set HttpOnly flag on all security cookies

Call cookie.setHttpOnly(true) immediately after creating every Cookie object to prevent JavaScript access.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-xxe-disable -->
### sec-xxe-disable — Disable DOCTYPE and external entities on XML parsers

Set disallow-doctype-decl=true and disable external-general-entities and external-parameter-entities on SAXParserFactory/DocumentBuilderFactory before parsing untrusted XML.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-db-role-lookup -->
### sec-db-role-lookup — Derive authorization roles from server-side DB, not client cookies

Never decode client-supplied values for RBAC decisions; always look up roles from the database using a server-validated user ID.
<!-- /harness-remediation -->
