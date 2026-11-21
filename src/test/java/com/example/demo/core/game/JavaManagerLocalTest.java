package com.example.demo.core.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 本机 JDK 识别：按大版本挑，CRaC 跳过，多版本取版本号最大的。 */
class JavaManagerLocalTest {

    private static File fakeJdk(Path root, String name, String version, boolean crac) throws Exception {
        File home = new File(root.toFile(), name);
        File bin = new File(home, "bin");
        assertNotNull(bin.mkdirs());
        Files.writeString(new File(bin, "java.exe").toPath(), "", StandardCharsets.UTF_8);
        String release = "JAVA_VERSION=\"" + version + "\"\n" + (crac ? "CRAC=1\n" : "");
        Files.writeString(new File(home, "release").toPath(), release, StandardCharsets.UTF_8);
        return home;
    }

    @Test
    void picksMatchingMajorVersion(@TempDir Path temp) throws Exception {
        File jdk17 = fakeJdk(temp, "jdk-17.0.2", "17.0.2", false);
        File jdk21 = fakeJdk(temp, "jdk-21.0.11", "21.0.11", false);
        File picked = JavaManager.findLocalJavaIn(List.of(jdk17, jdk21), 21);
        assertNotNull(picked, "应当选中 21");
        assertEquals(new File(jdk21, "bin\\java.exe").getAbsolutePath(), picked.getAbsolutePath());
        assertNull(JavaManager.findLocalJavaIn(List.of(jdk17), 21), "只有 17 时不能拿来跑 21");
    }

    @Test
    void skipsCracBuildsAndPrefersNewest(@TempDir Path temp) throws Exception {
        File crac21 = fakeJdk(temp, "jdk-21.0.99", "21.0.99", true);
        File older21 = fakeJdk(temp, "jdk-21.0.5.1", "21.0.5.1", false);
        File newer21 = fakeJdk(temp, "jdk-21.0.13.1", "21.0.13.1", false);
        File picked = JavaManager.findLocalJavaIn(List.of(crac21, older21, newer21), 21);
        assertNotNull(picked);
        assertEquals(new File(newer21, "bin\\java.exe").getAbsolutePath(), picked.getAbsolutePath(),
                "CRaC 跳过，且在标准构建里取版本号最大的");
    }

    @Test
    void directoryWithoutReleaseIsIgnored(@TempDir Path temp) throws Exception {
        File broken = new File(temp.toFile(), "jdk-21.0.1");
        assertNotNull(new File(broken, "bin").mkdirs());
        Files.writeString(new File(broken, "bin\\java.exe").toPath(), "", StandardCharsets.UTF_8);
        assertNull(JavaManager.findLocalJavaIn(List.of(broken), 21), "读不到 release 就当没装");
    }
}
