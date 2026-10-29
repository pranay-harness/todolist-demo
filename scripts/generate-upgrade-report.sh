#!/bin/bash
# scripts/generate-upgrade-report.sh
# Generate comprehensive upgrade report

LIBRARY=$1
OLD_VERSION=$2
NEW_VERSION=$3

SAFE_LIB_NAME=${LIBRARY//:/--}
REPORT_FILE="target/upgrade-report-${SAFE_LIB_NAME}-${NEW_VERSION}.md"

mkdir -p target

cat > "$REPORT_FILE" << EOF
# Upgrade Report: $LIBRARY

**Old Version:** $OLD_VERSION
**New Version:** $NEW_VERSION
**Date:** $(date)
**Branch:** $(git branch --show-current 2>/dev/null || echo "N/A")

## Executive Summary

This report documents the upgrade of $LIBRARY from version $OLD_VERSION to $NEW_VERSION,
including breaking changes detected, test results, and recommendations.

## Breaking Changes Detected

EOF

# Add japicmp results if available
if [ -f "target/japicmp-report.xml" ]; then
    echo "### API Compatibility Analysis" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Japicmp analysis was performed to detect API changes." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Check for incompatibilities
    if grep -q "BINARY_INCOMPATIBLE" target/japicmp-report.xml 2>/dev/null; then
        echo "⚠️ **Binary incompatibilities detected**" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "See [japicmp-report.html](japicmp-report.html) for detailed analysis." >> "$REPORT_FILE"
    else
        echo "✓ **No binary incompatibilities detected**" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "See [japicmp-report.html](japicmp-report.html) for full analysis." >> "$REPORT_FILE"
    fi
    echo "" >> "$REPORT_FILE"
else
    echo "⚠️ API compatibility analysis was not performed (JARs not available)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
fi

# Add deprecated API usage results
if [ -f "target/deprecated-usage.log" ]; then
    echo "### Deprecated API Usage" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "\`\`\`" >> "$REPORT_FILE"
    cat target/deprecated-usage.log >> "$REPORT_FILE"
    echo "\`\`\`" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
fi

# Add test results
echo "### Test Results" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ -d "target/surefire-reports" ]; then
    TEST_COUNT=$(find target/surefire-reports -name "TEST-*.xml" 2>/dev/null | wc -l)
    if [ $TEST_COUNT -gt 0 ]; then
        echo "✓ Test suite executed ($TEST_COUNT test files)" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "For detailed test results, see:" >> "$REPORT_FILE"
        echo "- Surefire reports: \`target/surefire-reports/\`" >> "$REPORT_FILE"
        echo "- Test report: \`target/site/surefire-report.html\`" >> "$REPORT_FILE"
    else
        echo "⚠️ No test results found" >> "$REPORT_FILE"
    fi
else
    echo "⚠️ Test results not available" >> "$REPORT_FILE"
fi
echo "" >> "$REPORT_FILE"

# Add SCA results
echo "### Security Scan Results" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ -f "target/sca-results.json" ]; then
    echo "SCA scan was performed on the updated dependencies." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Review the SCA output for vulnerability details." >> "$REPORT_FILE"
else
    echo "⚠️ SCA scan results not available" >> "$REPORT_FILE"
fi
echo "" >> "$REPORT_FILE"

# Add recommendations
cat >> "$REPORT_FILE" << EOF
## Recommendations

1. **Review Breaking Changes**: Examine the japicmp report for any API changes that affect your code
2. **Update Deprecated APIs**: If deprecated API usage was detected, update to use new APIs
3. **Run Integration Tests**: Perform full integration testing in a staging environment
4. **Manual Testing**: Manually test critical functionality that may be affected
5. **Performance Testing**: Run performance benchmarks to detect any regressions
6. **Monitor in Production**: After deployment, monitor application logs and metrics closely

## Code Changes Required

EOF

# Add specific code change recommendations based on library
case "$LIBRARY" in
    *commons-fileupload*)
        cat >> "$REPORT_FILE" << EOF
For commons-fileupload 1.2.2 → 1.6.0:

\`\`\`java
// Add file count limit configuration (new in 1.5+)
DiskFileItemFactory factory = new DiskFileItemFactory();
factory.setFileCountMax(1000); // Prevent DoS
ServletFileUpload upload = new ServletFileUpload(factory);
upload.setFileCountMax(1000);
\`\`\`
EOF
        ;;
    *commons-beanutils*)
        cat >> "$REPORT_FILE" << EOF
For commons-beanutils 1.8.3 → 1.11.0:

Note: Class property access is now restricted by default. If you need the old unsafe behavior:

\`\`\`java
PropertyUtilsBean propertyUtils = new PropertyUtilsBean();
propertyUtils.removeBeanIntrospector(SuppressPropertiesBeanIntrospector.SUPPRESS_CLASS);
\`\`\`

**This is not recommended for security reasons.**
EOF
        ;;
    *jackson-databind*)
        cat >> "$REPORT_FILE" << EOF
For jackson-databind 2.9.8 → 2.12.0:

Safe typing is now enabled by default. If you need to allow specific types:

\`\`\`java
ObjectMapper mapper = new ObjectMapper();
mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
\`\`\`

**Be cautious when enabling default typing as it can introduce security risks.**
EOF
        ;;
    *mysql-connector*)
        cat >> "$REPORT_FILE" << EOF
For mysql-connector-java 5.1.47 → 8.0.33:

\`\`\`java
// Update JDBC URL
String url = "jdbc:mysql://localhost:3306/db?serverTimezone=UTC&useSSL=false";

// Update driver class
Class.forName("com.mysql.cj.jdbc.Driver");
\`\`\`
EOF
        ;;
esac

# Add rollback plan
cat >> "$REPORT_FILE" << EOF

## Rollback Plan

If issues are detected after deployment:

1. **Immediate Rollback**:
   \`\`\`bash
   git revert <commit-hash>
   mvn clean deploy
   \`\`\`

2. **Alternative Rollback**:
   \`\`\`bash
   git checkout <previous-branch>
   mvn clean deploy
   \`\`\`

3. **Database Migrations**: If the upgrade includes database changes, ensure rollback scripts are available

## Next Steps

1. Review this report thoroughly
2. Address any breaking changes or deprecated API usage
3. Run full integration tests in staging environment
4. Get approval from team lead
5. Schedule deployment during low-traffic period
6. Monitor application closely after deployment

## Contact

For questions or concerns about this upgrade, contact the security team or the development lead.

---
**Report Generated:** $(date)
**Generated By:** Dependency Upgrade Automation
EOF

echo "Report generated: $REPORT_FILE"
echo ""
echo "View the report:"
echo "  cat $REPORT_FILE"
echo "  open $REPORT_FILE"
