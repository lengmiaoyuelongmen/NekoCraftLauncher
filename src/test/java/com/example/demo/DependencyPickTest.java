package com.example.demo;

import com.example.demo.core.mod.DependencyPick;
import com.example.demo.core.mod.VersionFit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 自动补前置时从版本列表里挑文件。 */
class DependencyPickTest {

    private static final String TWO_VERSIONS = """
            [
              {
                "version_number": "15.0.0",
                "version_type": "release",
                "game_versions": ["26.3"],
                "loaders": ["fabric"],
                "files": [
                  {"url": "https://cdn/26.3.jar", "filename": "mod-15.0.0.jar", "size": 100, "primary": true}
                ]
              },
              {
                "version_number": "11.1.2",
                "version_type": "release",
                "game_versions": ["1.20.1"],
                "loaders": ["fabric"],
                "files": [
                  {"url": "https://cdn/1.20.1.jar", "filename": "mod-11.1.2.jar", "size": 90, "primary": true}
                ]
              }
            ]
            """;

    private VersionFit.Target target(String mc, VersionFit.Loader loader) {
        return VersionFit.Target.of(mc, loader);
    }

    @Test
    void picksTheVersionMatchingTheInstance() {
        DependencyPick.Artifact artifact =
                DependencyPick.pick(TWO_VERSIONS, target("1.20.1", VersionFit.Loader.FABRIC));
        assertNotNull(artifact);
        assertEquals("11.1.2", artifact.versionNumber());
        assertEquals("https://cdn/1.20.1.jar", artifact.url());
        assertEquals("mod-11.1.2.jar", artifact.filename());
        assertEquals(90, artifact.size());
    }

    @Test
    void refusesWhenNothingMatches() {
        assertNull(DependencyPick.pick(TWO_VERSIONS, target("1.12.2", VersionFit.Loader.FABRIC)));
        assertNull(DependencyPick.pick(TWO_VERSIONS, target("1.12.2", VersionFit.Loader.FORGE)));
    }

    @Test
    void fallsBackToTheFirstFileWhenNoPrimaryFlag() {
        String json = """
                [
                  {
                    "version_number": "1.0",
                    "version_type": "release",
                    "game_versions": ["1.20.1"],
                    "loaders": [],
                    "files": [
                      {"url": "https://cdn/a.jar", "filename": "a.jar", "size": 5},
                      {"url": "https://cdn/b.jar", "filename": "b.jar", "size": 6}
                    ]
                  }
                ]
                """;
        DependencyPick.Artifact artifact = DependencyPick.pick(json, target("1.20.1", VersionFit.Loader.VANILLA));
        assertNotNull(artifact);
        assertEquals("a.jar", artifact.filename());
    }

    @Test
    void malformedInputIsIgnored() {
        assertNull(DependencyPick.pick(null, target("1.20.1", VersionFit.Loader.VANILLA)));
        assertNull(DependencyPick.pick("   ", target("1.20.1", VersionFit.Loader.VANILLA)));
        assertNull(DependencyPick.pick("not json", target("1.20.1", VersionFit.Loader.VANILLA)));
        assertNull(DependencyPick.pick("[]", target("1.20.1", VersionFit.Loader.VANILLA)));
        assertNull(DependencyPick.pick("[{\"version_number\":\"1\"}]", target("1.20.1", VersionFit.Loader.VANILLA)),
                "没有 files 时不应崩");
    }

    @Test
    void urlsAreBuiltForOfficialAndMirror() {
        assertTrue(DependencyPick.officialUrl("cloth-config").contains("api.modrinth.com"));
        assertTrue(DependencyPick.officialUrl("cloth-config").endsWith("/cloth-config/version"));
        assertTrue(DependencyPick.mirrorUrl("cloth-config").contains("mcimirror"));
    }
}
