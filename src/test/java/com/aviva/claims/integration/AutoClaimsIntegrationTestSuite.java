// Test Suite Runner
package com.aviva.claims.integration;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Aviva Auto Claims Integration Test Suite")
@SelectPackages("com.aviva.claims.integration")
@IncludeClassNamePatterns(".*IntegrationTest|.*Test")
public class AutoClaimsIntegrationTestSuite {
    // This class serves as a test suite runner
    // All integration tests will be executed when this suite runs
}

// Test Execution Report Generator
