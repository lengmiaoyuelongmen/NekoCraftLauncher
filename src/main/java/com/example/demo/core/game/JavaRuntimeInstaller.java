package com.example.demo.core.game;

import com.example.demo.core.download.ZipUtil;
import com.example.demo.core.instance.InstanceFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 把下载好的 JDK 压缩包安装成 {@code game/java/<name>/} 运行时目录。
 *
 * <p>JDK 压缩包通常带一层顶层目录（{@code jdk-21.0.13+9/} 或 {@code zulu21.../}），
 * 这里会自动把它剥掉，让最终目录直接是 {@code bin/java.exe}。
 */
public final class JavaRuntimeInstaller {

    private JavaRuntimeInstaller() {
    }

    /**
     * 解压并规范化目录。
     *
     * @param zip        下载好的 zip
     * @param runtimeDir 目标运行时目录（例如 {@code game/java/zulu21}）
     * @return 找到的 {@code bin/java.exe}
     */
    public static File install(File zip, File runtimeDir) throws IOException {
        File parent = runtimeDir.getParentFile();
        if (parent == null) {
            throw new IOException("运行时目录不合法: " + runtimeDir);
        }
        File temp = new File(parent, runtimeDir.getName() + ".tmp");
        InstanceFiles.deleteTree(temp);
        if (!temp.mkdirs() && !temp.isDirectory()) {
            throw new IOException("无法创建临时目录: " + temp);
        }
        ZipUtil.unzip(zip, temp);

        File root = singleDirectory(temp);
        InstanceFiles.deleteTree(runtimeDir);
        if (!runtimeDir.getParentFile().isDirectory() && !runtimeDir.getParentFile().mkdirs()) {
            throw new IOException("无法创建运行时目录: " + runtimeDir);
        }
        File source = root != null ? root : temp;
        // 同盘 rename 最快；失败再退回复制
        if (!source.renameTo(runtimeDir)) {
            try {
                com.example.demo.core.instance.InstanceFiles.copyTree(source, runtimeDir, java.util.Set.of());
            } catch (Exception copyFailed) {
                throw new IOException("移动运行时目录失败: " + copyFailed.getMessage(), copyFailed);
            }
        }
        InstanceFiles.deleteTree(temp);

        File javaExe = new File(new File(runtimeDir, "bin"), "java.exe");
        if (!javaExe.isFile()) {
            throw new IOException("解压后未找到 bin/java.exe: " + runtimeDir.getAbsolutePath());
        }
        return javaExe;
    }

    /** zip 里如果只有一个顶层目录且没有散文件，返回它（用于剥掉 JDK 根目录）。 */
    static File singleDirectory(File dir) {
        File[] children = dir.listFiles();
        if (children == null || children.length != 1 || !children[0].isDirectory()) {
            return null;
        }
        return children[0];
    }

    /** SHA-256，用于校验下载的 JDK（Adoptium 提供 checksum）。下载失败或读不了时返回空串。 */
    public static String sha256(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
