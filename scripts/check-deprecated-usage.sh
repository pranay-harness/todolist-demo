#!/bin/bash
# scripts/check-deprecated-usage.sh
# Check for deprecated API usage in the codebase

LIBRARY=$1
VERSION=$2

ARTIFACT_ID=$(echo "$LIBRARY" | cut -d: -f2)

echo "Checking for deprecated API usage from $LIBRARY $VERSION"

# Find all Java files
JAVA_FILES=$(find src/main/java -name "*.java" -type f 2>/dev/null || true)

if [ -z "$JAVA_FILES" ]; then
    echo "No Java files found in src/main/java"
    exit 0
fi

DEPRECATED_COUNT=0

for file in $JAVA_FILES; do
    # Check for imports from the library
    if grep -q "import.*$ARTIFACT_ID" "$file" 2>/dev/null; then
        echo "Found usage in: $file"

        # Check for @Deprecated annotations in the file
        if grep -q "@Deprecated" "$file" 2>/dev/null; then
            echo "  ⚠️  Contains @Deprecated annotation"
            DEPRECATED_COUNT=$((DEPRECATED_COUNT + 1))
        fi

        # Check for common deprecation patterns
        # This is a simplified check - in production, use more sophisticated tools
        if grep -i "deprecated" "$file" 2>/dev/null; then
            echo "  ℹ️  Contains 'deprecated' keyword"
        fi
    fi
done

if [ $DEPRECATED_COUNT -gt 0 ]; then
    echo ""
    echo "⚠️  Found $DEPRECATED_COUNT files with potential deprecated API usage"
    echo "Review these files and update to use new APIs if needed."
else
    echo "✓ No obvious deprecated API usage detected"
fi

# Suggest using more sophisticated tools
echo ""
echo "For comprehensive deprecation analysis, consider using:"
echo "  - Error Prone: https://errorprone.info/"
echo "  - SpotBugs: https://spotbugs.github.io/"
echo "  - Deprecation Detector: https://github.com/molikto/deprecation-detector"
