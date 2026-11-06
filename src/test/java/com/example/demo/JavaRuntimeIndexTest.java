package com.example.demo;

import com.example.demo.core.game.JavaRuntimeIndex;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Java 运行时下载源：选包规则（zip / 非 CRaC / 优先非 fx / Adoptium checksum）。 */
class JavaRuntimeIndexTest {

    @Test
    void zuluPrefersPlainZip() {
        String json = """
                [
                  {"download_url": "https://cdn.azul.com/zulu/bin/zulu21.msi"},
                  {"download_url": "https://cdn.azul.com/zulu/bin/zulu21-crac.zip"},
                  {"download_url": "https://cdn.azul.com/zulu/bin/zulu21-ca-fx-jdk21-win_x64.zip"},
                  {"download_url": "https://cdn.azul.com/zulu/bin/zulu21-ca-jdk21-win_x64.zip"}
                ]
                """;

        JavaRuntimeIndex.Package pkg = JavaRuntimeIndex.parseZulu(
                JsonParser.parseString(json).getAsJsonArray());

        assertEquals("https://cdn.azul.com/zulu/bin/zulu21-ca-jdk21-win_x64.zip", pkg.url(),
                "应跳过 msi / CRaC / 带 JavaFX 的包");
        assertEquals("zulu21-ca-jdk21-win_x64.zip", pkg.fileName());
        assertEquals("Azul Zulu", pkg.source());
    }

    @Test
    void zuluFallsBackToFxBuild() {
        String json = """
                [{"download_url": "https://cdn.azul.com/zulu/bin/zulu21-ca-fx-jdk21-win_x64.zip"}]
                """;

        JavaRuntimeIndex.Package pkg = JavaRuntimeIndex.parseZulu(
                JsonParser.parseString(json).getAsJsonArray());

        assertEquals("https://cdn.azul.com/zulu/bin/zulu21-ca-fx-jdk21-win_x64.zip", pkg.url(),
                "只有 fx 包时也要能用");
    }

    @Test
    void zuluReturnsNullWhenNothingUsable() {
        assertNull(JavaRuntimeIndex.parseZulu(JsonParser.parseString("[]").getAsJsonArray()));
        assertNull(JavaRuntimeIndex.parseZulu(JsonParser.parseString(
                "[{\"download_url\": \"https://cdn.azul.com/zulu/bin/zulu21.msi\"}]").getAsJsonArray()));
    }

    @Test
    void adoptiumParsesLinkAndChecksum() {
        String json = """
                [
                  {"binary": {"package": {
                     "name": "OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip",
                     "link": "https://github.com/adoptium/temurin21-binaries/releases/download/x.zip",
                     "checksum": "a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9"}}}
                ]
                """;

        JavaRuntimeIndex.Package pkg = JavaRuntimeIndex.parseAdoptium(
                JsonParser.parseString(json).getAsJsonArray());

        assertEquals("https://github.com/adoptium/temurin21-binaries/releases/download/x.zip", pkg.url());
        assertEquals("a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9", pkg.sha256());
        assertEquals("OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip", pkg.fileName());
        assertEquals("Adoptium Temurin", pkg.source());
    }

    @Test
    void adoptiumSkipsEntriesWithoutPackage() {
        assertNull(JavaRuntimeIndex.parseAdoptium(JsonParser.parseString(
                "[{\"binary\": {}}, {\"binary\": {\"package\": {}}}]").getAsJsonArray()));
    }
}
