package com.example.demo;

import com.example.demo.core.meta.VersionProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 版本 JSON 的 rules 求值与库路径解析（启动器的地基）。 */
class VersionProfileRulesTest {

    private JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void noRulesMeansAllowed() {
        assertTrue(VersionProfile.allowed(json("{}")));
    }

    @Test
    void allowRuleMustMatch() {
        JsonObject rule = json("""
                {"rules":[{"action":"allow","features":{"is_demo_user":true}}]}
                """);
        assertFalse(VersionProfile.allowed(rule, Map.of("is_demo_user", false)),
                "allow 规则不命中 → 排除");
        assertTrue(VersionProfile.allowed(rule, Map.of("is_demo_user", true)));
    }

    @Test
    void disallowRuleThatMatchesExcludes() {
        JsonObject rule = json("""
                {"rules":[{"action":"disallow","features":{"is_demo_user":true}}]}
                """);
        assertFalse(VersionProfile.allowed(rule, Map.of("is_demo_user", true)),
                "disallow 命中必须排除（旧实现错误地保留了它）");
    }

    @Test
    void disallowOnlyRuleKeepsWhenNotMatched() {
        JsonObject rule = json("""
                {"rules":[{"action":"disallow","os":{"name":"osx"}}]}
                """);
        assertTrue(VersionProfile.allowed(rule, Map.of()),
                "只有 disallow 规则且不命中时保留（「除 macOS 外都可用」）");
    }

    @Test
    void lastMatchingRuleWins() {
        JsonObject rule = json("""
                {"rules":[
                  {"action":"allow"},
                  {"action":"disallow","features":{"is_demo_user":true}}
                ]}
                """);
        assertTrue(VersionProfile.allowed(rule, Map.of("is_demo_user", false)));
        assertFalse(VersionProfile.allowed(rule, Map.of("is_demo_user", true)),
                "后面的 disallow 命中会覆盖前面的 allow");
    }

    @Test
    void libraryPathFromDownloadsOrCoordinates() {
        JsonObject withArtifact = json("""
                {"name":"com.example:lib:1.0",
                 "downloads":{"artifact":{"path":"com/example/lib/1.0/lib-1.0.jar"}}}
                """);
        assertEquals("com/example/lib/1.0/lib-1.0.jar", VersionProfile.libraryPath(withArtifact));

        JsonObject coordinates = json("{\"name\":\"org.example:mod:2.3\"}");
        assertEquals("org/example/mod/2.3/mod-2.3.jar", VersionProfile.libraryPath(coordinates));

        JsonObject withClassifier = json("{\"name\":\"org.example:mod:2.3:natives-windows\"}");
        assertEquals("org/example/mod/2.3/mod-2.3-natives-windows.jar",
                VersionProfile.libraryPath(withClassifier));

        assertNull(VersionProfile.libraryPath(json("{\"name\":\"broken\"}")));
    }

    @Test
    void classpathAndNativeLibrariesSplitCorrectly(@TempDir Path temp) throws Exception {
        Path versionDir = Files.createDirectories(temp.resolve("versions").resolve("t"));
        Files.writeString(versionDir.resolve("t.json"), """
                {
                  "id":"t","type":"release","mainClass":"x",
                  "libraries":[
                    {"name":"a:a:1", "downloads":{"artifact":{"path":"a/a/1/a-1.jar"}}},
                    {"name":"org.lwjgl:lwjgl:3.2.2",
                     "natives":{"windows":"natives-windows"},
                     "downloads":{"classifiers":{"natives-windows":{"path":"x/natives.jar"}}}},
                    {"name":"b:b:1","rules":[{"action":"allow","features":{"is_demo_user":true}}],
                     "downloads":{"artifact":{"path":"b/b/1/b-1.jar"}}}
                  ]
                }
                """, StandardCharsets.UTF_8);

        VersionProfile profile = VersionProfile.load(temp.toFile(), "t");

        List<JsonObject> classpath = profile.classpathLibraries();
        assertEquals(1, classpath.size(), "natives-only 与未命中 rules 的库不进 classpath");

        List<JsonObject> natives = profile.nativeLibraries();
        assertEquals(1, natives.size(), "带 natives 信息的库才需要解压");
        assertEquals("org.lwjgl:lwjgl:3.2.2", natives.get(0).get("name").getAsString());
    }
}
