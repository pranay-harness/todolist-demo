Instructions to the Remediation agent:

Do not upgrade the dependency httpclient even if there are vulnerabilities as this is used for testing purpose



<!-- harness-remediation:sec-path-traversal -->
### sec-path-traversal — Canonicalize paths to prevent directory traversal

User-supplied file paths must be resolved against a canonical root with Path.normalize() and startsWith() check; reject any path that escapes the intended directory.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-sql-params -->
### sec-sql-params — Use PreparedStatement placeholders for SQL

Replace string-concatenated SQL with '?' placeholders bound via Spring JdbcTemplate; never build SQL by concatenating user input directly.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-cmd-inject -->
### sec-cmd-inject — Use ProcessBuilder list args for shell commands

Pass command arguments as a discrete list to ProcessBuilder (not via sh -c) so user input is treated as a literal argument and cannot inject shell metacharacters.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-xxe-disable -->
### sec-xxe-disable — Disable DOCTYPE and external entities on XML parsers

Always set disallow-doctype-decl=true plus disable external-general-entities and external-parameter-entities on SAXParserFactory before parsing attacker-supplied XML.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-ssrf-allowlist -->
### sec-ssrf-allowlist — Allowlist URL hosts before server-side HTTP requests

Validate the target hostname against a static ALLOWED_HOSTS set before any server-side HTTP fetch; reject all other hosts to prevent SSRF.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-xss-encode -->
### sec-xss-encode — HTML-encode user input before HTML response insertion

Always call StringEscapeUtils.escapeHtml4() on attacker-controlled values before inserting them into HTML responses; a naive blocklist filter is not sufficient.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-secure-random -->
### sec-secure-random — Use SecureRandom for security-sensitive random values

Replace java.util.Random with java.security.SecureRandom for session tokens, OTPs, and file name nonces; never seed from System.currentTimeMillis().
<!-- /harness-remediation -->

<!-- harness-remediation:sec-crlf-strip -->
### sec-crlf-strip — Strip CRLF characters from HTTP header values

Apply replaceAll('[\r\n]', '') to any user-supplied string before placing it in an HTTP header to prevent response-splitting and log injection.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-httponly-cookie -->
### sec-httponly-cookie — Set HttpOnly on security-sensitive cookies

Always call cookie.setHttpOnly(true) on session and auth cookies to prevent JavaScript access; also set Secure on HTTPS-served apps.
<!-- /harness-remediation -->

<!-- harness-remediation:sec-env-credentials -->
### sec-env-credentials — Load credentials from environment variables

Never hardcode passwords, API keys, or keystore credentials; read them from System.getenv() at startup and fail closed if the variable is absent.
<!-- /harness-remediation -->
