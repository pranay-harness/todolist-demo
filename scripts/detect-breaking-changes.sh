#!/bin/bash
# scripts/detect-breaking-changes.sh
# Detect breaking changes using japicmp and static analysis

LIBRARY=$1
OLD_VERSION=$2
NEW_VERSION=$3

ARTIFACT_ID=$(echo "$LIBRARY" | cut -d: -f2)
SAFE_ARTIFACT_ID=${ARTIFACT_ID//./-}

echo "Detecting breaking changes for $LIBRARY: $OLD_VERSION → $NEW_VERSION"

# Create target directory
mkdir -p target/jars

# Download JARs using Maven
echo "Downloading JARs for comparison..."

# Download old version
mvn dependency:get -Dartifact="$LIBRARY:$OLD_VERSION" -DremoteRepositories=https://repo1.maven.org/maven2 || {
    echo "Failed to download old version $OLD_VERSION"
}

# Download new version
mvn dependency:get -Dartifact="$LIBRARY:$NEW_VERSION" -DremoteRepositories=https://repo1.maven.org/maven2 || {
    echo "Failed to download new version $NEW_VERSION"
}

# Copy JARs from local Maven repo
GROUP_PATH=$(echo "$LIBRARY" | cut -d: -f1 | tr . /)
OLD_JAR="$HOME/.m2/repository/$GROUP_PATH/$ARTIFACT_ID/$OLD_VERSION/$ARTIFACT_ID-$OLD_VERSION.jar"
NEW_JAR="$HOME/.m2/repository/$GROUP_PATH/$ARTIFACT_ID/$NEW_VERSION/$ARTIFACT_ID-$NEW_VERSION.jar"

if [ -f "$OLD_JAR" ] && [ -f "$NEW_JAR" ]; then
    echo "JARs found in local Maven repository"
    cp "$OLD_JAR" "target/jars/"
    cp "$NEW_JAR" "target/jars/"
else
    echo "JARs not found in local repository. Attempting to download from Maven Central..."
    # Try downloading directly
    curl -L -o "target/jars/$ARTIFACT_ID-$OLD_VERSION.jar" \
        "https://repo1.maven.org/maven2/$GROUP_PATH/$ARTIFACT_ID/$OLD_VERSION/$ARTIFACT_ID-$OLD_VERSION.jar" || true
    curl -L -o "target/jars/$ARTIFACT_ID-$NEW_VERSION.jar" \
        "https://repo1.maven.org/maven2/$GROUP_PATH/$ARTIFACT_ID/$NEW_VERSION/$ARTIFACT_ID-$NEW_VERSION.jar" || true
fi

OLD_JAR="target/jars/$ARTIFACT_ID-$OLD_VERSION.jar"
NEW_JAR="target/jars/$ARTIFACT_ID-$NEW_VERSION.jar"

# Run japicmp if JARs are available
if [ -f "$OLD_JAR" ] && [ -f "$NEW_JAR" ]; then
    echo "Running japicmp analysis..."

    # Download japicmp if not available
    if [ ! -f "target/japicmp.jar" ]; then
        echo "Downloading japicmp..."
        curl -L -o target/japicmp.jar \
            https://repo1.maven.org/maven2/com/github/siom79/japicmp/japicmp/0.18.3/japicmp-0.18.3-jar-with-dependencies.jar
    fi

    java -jar target/japicmp.jar \
        -o "$OLD_JAR" \
        -n "$NEW_JAR" \
        --only-modified \
        --only-binary-incompatible \
        --html target/japicmp-report.html \
        --xml target/japicmp-report.xml \
        --only-code-incompatible \
        --ignore-missing-classes || true

    if [ -f "target/japicmp-report.html" ]; then
        echo "Japicmp report generated: target/japicmp-report.html"

        # Check for binary incompatibilities
        if grep -q "BINARY_INCOMPATIBLE" target/japicmp-report.xml 2>/dev/null; then
            echo "⚠️  WARNING: Binary incompatibilities detected!"
            echo "Review target/japicmp-report.html for details."
        else
            echo "✓ No binary incompatibilities detected."
        fi
    fi
else
    echo "⚠️  Could not download JARs for japicmp analysis"
    echo "Falling back to changelog analysis..."
fi

# Check for deprecated API usage in code
echo ""
echo "Checking for deprecated API usage..."
./scripts/check-deprecated-usage.sh "$LIBRARY" "$NEW_VERSION" || true

echo ""
echo "Breaking change detection complete."
