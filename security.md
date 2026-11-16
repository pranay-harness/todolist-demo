# Security

Security rules for this repository. Marked blocks are maintained by Harness remediation. Text outside those blocks is left as written.


<!-- harness-remediation:sec-actions-sha-pin -->
### sec-actions-sha-pin — Pin GitHub Actions to commit SHAs

Workflows in this repo reference third-party actions by full 40-character commit SHA, with the original tag kept as a trailing comment, instead of mutable tags like @v4. Resolve the SHA with git ls-remote (use the peeled commit for annotated tags) and bump it deliberately.
<!-- /harness-remediation -->
