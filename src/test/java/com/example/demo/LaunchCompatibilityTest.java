package com.example.demo;

import com.example.demo.core.launch.LaunchCompatibility;
import com.example.demo.core.launch.LaunchCompatibility.Issue;
import com.example.demo.core.launch.LaunchCompatibility.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 启动前的兼容性判断：Java 版本、客户端 jar、版本文件。 */
class LaunchCompatibilityTest {

    @Test
    void healthySetupHasNoIssues() {
        List<Issue> issues = LaunchCompatibility.check(8, 8, "C:/java/bin/java.exe", true, true);
        assertTrue(issues.isEmpty());
        assertFalse(LaunchCompatibility.blocking(issues));
        assertEquals("", LaunchCompatibility.describe(issues));
    }

    @Test
    void newerJavaIsFine() {
        assertTrue(LaunchCompatibility.check(8, 17, "java", true, true).isEmpty());
        assertTrue(LaunchCompatibility.check(17, 21, "java", true, true).isEmpty());
    }

    @Test
    void olderJavaBlocks() {
        List<Issue> issues = LaunchCompatibility.check(17, 8, "java", true, true);
        Issue first = LaunchCompatibility.firstBlocking(issues);
        assertEquals(Kind.JAVA_TOO_OLD, first.kind());
        assertTrue(first.message().contains("Java 17"));
        assertTrue(first.message().contains("Java 8"));
        assertTrue(LaunchCompatibility.blocking(issues));
    }

    @Test
    void missingJavaPathBlocks() {
        List<Issue> issues = LaunchCompatibility.check(8, 0, "  ", true, true);
        assertEquals(Kind.JAVA_PATH_INVALID, LaunchCompatibility.firstBlocking(issues).kind());
    }

    @Test
    void unknownJavaOnlyWarns() {
        List<Issue> issues = LaunchCompatibility.check(8, 0, "C:/java/bin/java.exe", true, true);
        assertEquals(1, issues.size());
        assertEquals(Kind.JAVA_UNKNOWN, issues.get(0).kind());
        assertEquals(LaunchCompatibility.Severity.WARN, issues.get(0).severity());
        assertFalse(LaunchCompatibility.blocking(issues));
    }

    @Test
    void missingClientJarBlocksWithRepairHint() {
        List<Issue> issues = LaunchCompatibility.check(8, 8, "java", false, true);
        Issue first = LaunchCompatibility.firstBlocking(issues);
        assertEquals(Kind.CLIENT_JAR_MISSING, first.kind());
        assertTrue(first.message().contains("校验库文件"));
    }

    @Test
    void missingVersionJsonBlocks() {
        List<Issue> issues = LaunchCompatibility.check(8, 8, "java", true, false);
        assertEquals(Kind.VERSION_JSON_MISSING, LaunchCompatibility.firstBlocking(issues).kind());
    }

    @Test
    void multipleIssuesAreAllReported() {
        List<Issue> issues = LaunchCompatibility.check(17, 8, "", false, true);
        assertEquals(3, issues.size());
        String text = LaunchCompatibility.describe(issues);
        assertTrue(text.contains("\n"), "多条问题应逐行列出");
        assertTrue(text.contains("Java 17"));
        assertTrue(text.contains("客户端 jar"));
    }

    @Test
    void noIssuesMeansNoBlocking() {
        assertFalse(LaunchCompatibility.blocking(List.of()));
        assertFalse(LaunchCompatibility.blocking(null));
        assertNull(LaunchCompatibility.firstBlocking(List.of()));
        assertEquals("", LaunchCompatibility.describe(null));
    }
    @Test
    void missingLibrariesBlockWithRepairHint() {
        List<Issue> issues = LaunchCompatibility.check(8, 8, "java", true, true, 7);
        Issue first = LaunchCompatibility.firstBlocking(issues);
        assertEquals(Kind.MISSING_LIBRARIES, first.kind());
        assertTrue(first.message().contains("7"));
        assertTrue(first.message().contains("校验库文件"));
    }

    @Test
    void noMissingLibrariesIsClean() {
        assertTrue(LaunchCompatibility.check(8, 8, "java", true, true, 0).isEmpty());
        assertTrue(LaunchCompatibility.check(8, 8, "java", true, true).isEmpty());
    }

    @Test
    void libraryProblemIsListedTogetherWithOthers() {
        List<Issue> issues = LaunchCompatibility.check(17, 8, "java", true, true, 2);
        assertEquals(2, issues.size());
        String text = LaunchCompatibility.describe(issues);
        assertTrue(text.contains("Java 17"));
        assertTrue(text.contains("2 个库文件"));
    }
}
