#!/bin/bash
# scripts/run-comprehensive-tests.sh
# Run comprehensive test suite for dependency upgrade validation

set -e

echo "=========================================="
echo "Running Comprehensive Test Suite"
echo "=========================================="

# Create target directory
mkdir -p target

# Step 1: Unit tests
echo ""
echo "Step 1: Running unit tests..."
mvn test || {
    echo "❌ Unit tests failed!"
    exit 1
}
echo "✓ Unit tests passed"

# Step 2: Integration tests
echo ""
echo "Step 2: Running integration tests..."
mvn verify || {
    echo "❌ Integration tests failed!"
    exit 1
}
echo "✓ Integration tests passed"

# Step 3: Security tests
echo ""
echo "Step 3: Running security tests..."
mvn test -Dtest=DependencyUpgradeTest || {
    echo "❌ Dependency upgrade tests failed!"
    exit 1
}
echo "✓ Dependency upgrade tests passed"

mvn test -Dtest=VulnerabilityValidationTest || {
    echo "❌ Vulnerability validation tests failed!"
    exit 1
}
echo "✓ Vulnerability validation tests passed"

# Step 4: Generate test report
echo ""
echo "Step 4: Generating test report..."
mvn surefire-report:report || {
    echo "⚠️  Test report generation failed (non-critical)"
}
echo "✓ Test report generated"

# Step 5: Performance benchmarks (placeholder)
echo ""
echo "Step 5: Running performance benchmarks..."
if [ -f "scripts/run-performance-benchmarks.sh" ]; then
    ./scripts/run-performance-benchmarks.sh || {
        echo "⚠️  Performance benchmarks failed (non-critical)"
    }
else
    echo "⚠️  Performance benchmark script not found (skipping)"
    echo "Note: Implement performance benchmarks based on your application requirements"
fi

# Step 6: Generate summary
echo ""
echo "=========================================="
echo "Test Suite Summary"
echo "=========================================="
echo "✓ Unit tests: PASSED"
echo "✓ Integration tests: PASSED"
echo "✓ Security tests: PASSED"
echo "✓ Test report: target/site/surefire-report.html"
echo "=========================================="
echo ""
echo "All tests passed successfully!"
echo "Review the test report for detailed results."
