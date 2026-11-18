Instructions to the Remediation agent:

Do not upgrade the dependency httpclient even if there are vulnerabilities as this is used for testing purpose



<!-- harness-remediation:sec-pem-boundary-split -->
### sec-pem-boundary-split — Split PEM boundary marker strings to avoid gitleaks false positives

PEM header/footer delimiter constants (e.g. BEGIN PRIVATE KEY) trigger the gitleaks private-key rule as false positives. Use Java compile-time string concatenation to split the literal so the exact matched string never appears in source while runtime value is identical.
<!-- /harness-remediation -->
