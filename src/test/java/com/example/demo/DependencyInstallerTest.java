package com.example.demo;

import com.example.demo.core.mod.DependencyInstaller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 已存在的依赖文件能否复用（防半截 jar）。 */
class DependencyInstallerTest {

    @Test
    void reusesWhenSizeMatches() {
        assertTrue(DependencyInstaller.reuseExisting(715518L, 715518L));
    }

    @Test
    void reuseWhenExpectedSizeUnknown() {
        assertTrue(DependencyInstaller.reuseExisting(1024L, 0L), "不知道期望大小时信任本地文件");
    }

    @Test
    void redownloadsWhenSizeDiffers() {
        assertFalse(DependencyInstaller.reuseExisting(1024L, 715518L), "半截文件必须重下");
        assertFalse(DependencyInstaller.reuseExisting(0L, 715518L), "空文件必须重下");
        assertFalse(DependencyInstaller.reuseExisting(0L, 0L), "空文件永不直接复用");
    }
}
