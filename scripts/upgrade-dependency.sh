#!/bin/bash
# scripts/upgrade-dependency.sh
# Automated dependency upgrade with breaking change detection

set -e

LIBRARY=$1
OLD_VERSION=$2
NEW_VERSION=$3

if [ -z "$LIBRARY" ] || [ -z "$OLD_VERSION" ] || [ -z "$NEW_VERSION" ]; then
    echo "Usage: $0 <library> <old-version> <new-version>"
    echo "Example: $0 commons-collections 3.2.1 3.2.2"
    echo ""
    echo "Library format: groupId:artifactId (e.g., commons-collections:commons-collections)"
    exit 1
fi

echo "=========================================="
echo "Upgrading $LIBRARY from $OLD_VERSION to $NEW_VERSION"
echo "=========================================="

# Step 1: Create backup branch
echo "Step 1: Creating backup branch..."
BRANCH_NAME="upgrade-${LIBRARY//:/--}-${OLD_VERSION}-to-${NEW_VERSION}"
git checkout -b "$BRANCH_NAME" || git checkout -b "$BRANCH_NAME" origin/main

# Step 2: Update pom.xml
echo "Step 2: Updating pom.xml..."
GROUP_ID=$(echo "$LIBRARY" | cut -d: -f1)
ARTIFACT_ID=$(echo "$LIBRARY" | cut -d: -f2)

# Backup original pom.xml
cp pom.xml pom.xml.backup

# Update version in pom.xml
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/<artifactId>$ARTIFACT_ID<\/artifactId>.*<version>$OLD_VERSION<\/version>/<artifactId>$ARTIFACT_ID<\/artifactId>\n    <version>$NEW_VERSION<\/version>/" pom.xml
else
    # Linux
    sed -i "s/<artifactId>$ARTIFACT_ID<\/artifactId>.*<version>$OLD_VERSION<\/version>/<artifactId>$ARTIFACT_ID<\/artifactId>\n    <version>$NEW_VERSION<\/version>/" pom.xml
fi

# Step 3: Run breaking change detection
echo "Step 3: Running breaking change detection..."
./scripts/detect-breaking-changes.sh "$LIBRARY" "$OLD_VERSION" "$NEW_VERSION" || true

# Step 4: Run tests
echo "Step 4: Running test suite..."
mvn clean test || {
    echo "Tests failed! Restoring original pom.xml..."
    mv pom.xml.backup pom.xml
    echo "Please review test failures and fix code before upgrading."
    exit 1
}

# Step 5: Run SCA scan
echo "Step 5: Running SCA scan..."
node "$HOME/.config/devin/skills/harness-sast-and-sca/run-sca-manifest.cjs" "pom.xml" > target/sca-results.json 2>&1 || true

# Step 6: Generate report
echo "Step 6: Generating upgrade report..."
./scripts/generate-upgrade-report.sh "$LIBRARY" "$OLD_VERSION" "$NEW_VERSION"

# Clean up backup
rm pom.xml.backup

echo "=========================================="
echo "Upgrade complete! Review the report before merging."
echo "Branch: $BRANCH_NAME"
echo "Report: target/upgrade-report-${LIBRARY//:/--}-${NEW_VERSION}.md"
echo "=========================================="
