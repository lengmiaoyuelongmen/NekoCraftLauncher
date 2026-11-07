package com.example.demo;

import com.example.demo.core.meta.LoaderCompatibility;
import com.example.demo.core.meta.LoaderCompatibility.Loader;
import com.example.demo.core.meta.LoaderCompatibility.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 加载器版本与 MC 版本的匹配判断。 */
class LoaderCompatibilityTest {

    @Test
    void forgeVersionCarriesItsMinecraftVersion() {
        assertTrue(LoaderCompatibility.validate(Loader.FORGE, "1.20.1", "1.20.1-47.4.10").ok());
        Result mismatch = LoaderCompatibility.validate(Loader.FORGE, "1.12.2", "1.20.1-47.4.10");
        assertFalse(mismatch.ok());
        assertTrue(mismatch.message().contains("1.20.1"));
        assertTrue(mismatch.message().contains("1.12.2"));
    }

    @Test
    void neoforgeUsesTheSamePrefixShape() {
        assertTrue(LoaderCompatibility.validate(Loader.NEOFORGE, "26.3", "26.3-66.1.0").ok());
        assertFalse(LoaderCompatibility.validate(Loader.NEOFORGE, "1.20.1", "26.3-66.1.0").ok());
    }

    @Test
    void bareBuildNumbersAreAccepted() {
        // 只给了 47.4.10 这种构建号，无法判断，放行
        assertTrue(LoaderCompatibility.validate(Loader.FORGE, "1.20.1", "47.4.10").ok());
    }

    @Test
    void fabricAndQuiltAreMinecraftAgnostic() {
        assertTrue(LoaderCompatibility.validate(Loader.FABRIC, "1.20.1", "0.15.11").ok());
        assertTrue(LoaderCompatibility.validate(Loader.QUILT, "26.3", "0.20.0").ok());
    }

    @Test
    void emptyVersionIsRejected() {
        Result empty = LoaderCompatibility.validate(Loader.FORGE, "1.20.1", "  ");
        assertFalse(empty.ok());
        assertTrue(empty.message().contains("Forge"));
    }

    @Test
    void unknownMcVersionSkipsThePrefixComparison() {
        assertTrue(LoaderCompatibility.validate(Loader.FORGE, "", "1.20.1-47.4.10").ok());
        assertTrue(LoaderCompatibility.validate(Loader.FORGE, null, "1.20.1-47.4.10").ok());
    }

    @Test
    void matchingFiltersTheList() {
        List<String> forge = List.of("1.20.1-47.4.10", "1.12.2-14.23.5.2860", "1.20.1-47.2.0");
        assertEquals(List.of("1.20.1-47.4.10", "1.20.1-47.2.0"),
                LoaderCompatibility.matching(Loader.FORGE, "1.20.1", forge));
        assertEquals(forge, LoaderCompatibility.matching(Loader.FABRIC, "1.20.1", forge),
                "Fabric 不应过滤");
        assertEquals(List.of(), LoaderCompatibility.matching(Loader.FORGE, "1.20.1", null));
    }

    @Test
    void labelParsing() {
        assertEquals(Loader.FABRIC, LoaderCompatibility.fromLabel("Fabric"));
        assertEquals(Loader.NEOFORGE, LoaderCompatibility.fromLabel(" neoforge "));
        assertNull(LoaderCompatibility.fromLabel("原版"));
        assertNull(LoaderCompatibility.fromLabel(null));
    }

    @Test
    void resultPassCarriesNoMessage() {
        Result pass = LoaderCompatibility.validate(Loader.FABRIC, "1.20.1", "0.15.11");
        assertTrue(pass.message().isEmpty());
    }
}
