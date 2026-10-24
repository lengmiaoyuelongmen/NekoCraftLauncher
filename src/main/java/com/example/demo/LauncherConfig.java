package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * 启动器配置。
 * 「设置」里改的内容会保存到程序工作目录下的 config.json（用 IDEA 运行时就是 demo 项目根目录）。
 */
public class LauncherConfig {

    /** 优先当前目录已有的 config.json；否则在 jar 旁边创建，保证双击启动也可移植。 */
    private static File resolveConfigFile() {
        File cwd = new File("config.json");
        File base = AppPaths.baseDir();
        File userDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        if (!base.getAbsoluteFile().equals(userDir)) {
            // 绿色版 / jpackage：配置跟启动器放一起；已有当前目录配置时优先它
            if (cwd.isFile()) return cwd;
            File target = AppPaths.dataDir();
            return new File(target, "config.json");
        }
        return cwd;
    }
    private static final File CONFIG_FILE = resolveConfigFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 最大堆内存，单位 MB */
    private int memoryMb = 2048;

    /** 自定义 java.exe 路径；空字符串表示交给启动器自动下载 Zulu 25 */
    private String javaPath = "";

    /** 游戏根目录（相当于官方的 .minecraft 目录） */
    private String gameDir = "game";

    /** 所有已添加的游戏目录（第一个是当前激活目录） */
    private java.util.List<String> gameDirs = new java.util.ArrayList<>();

    /** 目录别名：路径 -> 显示名 */
    private java.util.Map<String, String> gameDirNames = new java.util.LinkedHashMap<>();

    /** 收藏夹分组的展开状态：分类 -> 是否展开（持久化） */
    private java.util.Map<String, Boolean> favoriteGroups = new java.util.LinkedHashMap<>();

    /** 额外的 JVM 参数，例如 "-XX:+UseG1GC -XX:+AlwaysPreTouch" */
    private String jvmArgs = "";

    /** 版本隔离：每个实例独立 mods/config/saves（PCL 同名功能），默认开启 */
    private boolean isolation = true;

    /** 安装模组时自动补装缺失的前置依赖 */
    private boolean autoInstallDeps = true;

    /** 启用 JavaFX 硬件加速（关闭后用软件渲染，重启启动器生效） */
    private boolean hardwareAcceleration = true;

    /** 自动从本机检测并选择 Java（关闭后只用下面指定的路径 / 自动下载） */
    private boolean autoSelectJava = true;

    /** 下载源：auto（镜像优先）/ official（仅官方）/ mirror（仅镜像） */
    private String downloadSource = "auto";

    /** 最大并发下载连接数 */
    private int maxConcurrent = 16;

    /** 限速，单位 KB/s，0 表示不限速 */
    private int speedLimitKb = 0;

    /** 日志区保留的最大行数 */
    private int maxLogLines = 2000;

    /** 启动器窗口不透明度（0.7 ~ 1.0） */
    private double uiOpacity = 1.0;

    /** 界面动画（关掉相当于性能模式，低配机器更顺） */
    private boolean animations = true;

    /** 启动前自动检查并补全缺失的库文件 */
    private boolean autoRepairLibraries = true;

    /** 完整性检查时每个实例抽样多少个 assets 对象（0 = 不抽样） */
    private int assetSampleCount = 20;

    /** 内存自动配置（按本机内存推荐，忽略手动值） */
    private boolean autoMemory = true;

    /** 界面语言（zh_CN / en_US，目前只有中文完整支持） */
    private String uiLanguage = "zh_CN";

    /** 主题强调色名称（见 Theme.PRESETS） */
    private String accentColor = "共产红";

    /** 启动页背景图片路径（空 = 无） */
    private String backgroundImage = "";

    /** 界面字体缩放（1.0 = 标准） */
    private double uiFontScale = 1.0;

    /** 是否启用 DeepSeek AI（开启后命令栏输入直接发给模型） */
    private boolean aiEnabled;

    /** DeepSeek API Key（保存在 config.json，请勿分享该文件） */
    private String aiApiKey = "";

    /** DeepSeek 模型名 */
    private String aiModel = "deepseek-chat";

    /** DeepSeek API 基础地址 */
    private String aiBaseUrl = "https://api.deepseek.com";

    /** 首次启动是否已经询问过性能优化 */
    private boolean performanceAsked;

    /** 是否应用过性能优化（有备份时可恢复） */
    private boolean performanceApplied;

    /** 更新检查地址（静态 JSON，空 = 不检查） */
    private String updateUrl = "";

    /** 浅色模式（顶栏保持深色） */
    private boolean lightTheme = false;

    /** 智能性能：启动时自动合并 G1GC 参数并调整游戏进程优先级 */
    private boolean smartPerformance = false;

    /** 游戏进程优先级：normal / above / high */
    private String gamePriority = "normal";

    /** 启动游戏后自动最小化启动器 */
    private boolean minimizeOnLaunch = false;

    /** 游戏运行时暂停启动器实时背景，降低占用 */
    private boolean pauseVisualsInGame = true;

    /** 优先使用高性能独显（启动前写入 HKCU 显卡首选项） */
    private boolean preferDiscreteGpu = true;

    /** 动画速度倍率（0.6 ~ 2.0） */
    private double animationSpeed = 1.0;

    /** 上次使用的实例 id，下次启动自动选中 */
    private String lastInstance = "";

    /** 游戏窗口宽高（会通过 --width/--height 传给游戏） */
    private int windowWidth = 854;
    private int windowHeight = 480;

    /**
     * 读取配置；文件不存在或损坏时返回默认配置并立即写回一份。
     */
    public static LauncherConfig load() {
        if (CONFIG_FILE.isFile()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                LauncherConfig loaded = GSON.fromJson(reader, LauncherConfig.class);
                if (loaded != null) {
                    loaded.normalize();
                    return loaded;
                }
            } catch (Exception ignored) {
                // 配置损坏不影响启动，直接退回默认值
            }
        }
        LauncherConfig config = new LauncherConfig();
        config.save();
        return config;
    }

    /**
     * 写回 config.json。失败时静默忽略（设置仍然在本次运行中生效）。
     */
    public void save() {
        normalize();
        // 相对路径保存：发布目录整体移动后仍能找到 game
        String originalGameDir = gameDir;
        java.util.List<String> originalDirs = new java.util.ArrayList<>(gameDirs);
        File base = AppPaths.baseDir();
        gameDir = portablePath(gameDir, base);
        java.util.List<String> portableDirs = new java.util.ArrayList<>();
        for (String dir : gameDirs) portableDirs.add(portablePath(dir, base));
        gameDirs = portableDirs;
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (Exception ignored) {
            // 只读目录等极端情况下不阻断启动
        } finally {
            gameDir = originalGameDir;
            gameDirs = originalDirs;
        }
    }

    private static String portablePath(String path, File base) {
        if (path == null || path.isBlank() || base == null) return path;
        try {
            java.nio.file.Path value = java.nio.file.Path.of(path);
            if (!value.isAbsolute()) return path;
            java.nio.file.Path basePath = base.getAbsoluteFile().toPath();
            if (value.startsWith(basePath)) {
                return basePath.relativize(value).toString().replace("\\\\", "/");
            }
        } catch (Exception ignored) {
            // 保持原值
        }
        return path;
    }

    private void normalize() {
        if (memoryMb < 512) memoryMb = 512;
        if (memoryMb > 16384) memoryMb = 16384;
        if (javaPath == null) javaPath = "";
        if (jvmArgs == null) jvmArgs = "";
        if (gameDir == null || gameDir.isBlank()) gameDir = "game";
        if (gameDirs == null) gameDirs = new java.util.ArrayList<>();
        gameDirs.removeIf(dir -> dir == null || dir.isBlank());
        if (!gameDirs.contains(gameDir)) gameDirs.add(0, gameDir);
        if (gameDirs.size() > 12) gameDirs = new java.util.ArrayList<>(gameDirs.subList(0, 12));
        if (gameDirNames == null) gameDirNames = new java.util.LinkedHashMap<>();
        gameDirNames.entrySet().removeIf(entry -> entry.getKey() == null
                || entry.getValue() == null || entry.getValue().isBlank());
        if (favoriteGroups == null) favoriteGroups = new java.util.LinkedHashMap<>();
        if (assetSampleCount < 0) assetSampleCount = 0;
        if (assetSampleCount > 200) assetSampleCount = 200;
        if (windowWidth < 320) windowWidth = 854;
        if (windowHeight < 240) windowHeight = 480;
        if (downloadSource == null || downloadSource.isBlank()) downloadSource = "auto";
        if (maxConcurrent < 1) maxConcurrent = 16;
        if (maxConcurrent > 128) maxConcurrent = 128;
        if (speedLimitKb < 0) speedLimitKb = 0;
        if (uiOpacity < 0.7) uiOpacity = 1.0;
        if (uiOpacity > 1.0) uiOpacity = 1.0;
        if (animationSpeed < 0.5) animationSpeed = 1.0;
        if (animationSpeed > 2.0) animationSpeed = 2.0;
        if (lastInstance == null) lastInstance = "";
    }

    public int getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(int memoryMb) {
        this.memoryMb = memoryMb;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public String getGameDir() {
        return gameDir;
    }

    public void setGameDir(String gameDir) {
        this.gameDir = gameDir;
    }

    public java.util.List<String> getGameDirs() {
        return gameDirs;
    }

    public void setGameDirs(java.util.List<String> gameDirs) {
        this.gameDirs = gameDirs;
    }

    /** 添加一个游戏目录并去重。 */
    public void addGameDir(String dir) {
        if (gameDirs == null) gameDirs = new java.util.ArrayList<>();
        if (dir != null && !dir.isBlank() && !gameDirs.contains(dir)) {
            gameDirs.add(dir);
        }
    }

    public java.util.Map<String, String> getGameDirNames() {
        return gameDirNames;
    }

    public void setGameDirNames(java.util.Map<String, String> gameDirNames) {
        this.gameDirNames = gameDirNames;
    }

    public String gameDirName(String dir) {
        if (gameDirNames == null || dir == null) return "";
        return gameDirNames.getOrDefault(dir, "");
    }

    public void setGameDirName(String dir, String name) {
        if (gameDirNames == null) gameDirNames = new java.util.LinkedHashMap<>();
        if (name == null || name.isBlank()) gameDirNames.remove(dir);
        else gameDirNames.put(dir, name.trim());
    }

    public void removeGameDir(String dir) {
        if (gameDirs != null) gameDirs.remove(dir);
        if (gameDirNames != null) gameDirNames.remove(dir);
    }

    public java.util.Map<String, Boolean> getFavoriteGroups() {
        return favoriteGroups;
    }

    public void setFavoriteGroups(java.util.Map<String, Boolean> favoriteGroups) {
        this.favoriteGroups = favoriteGroups;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public boolean isAutoSelectJava() {
        return autoSelectJava;
    }

    public void setAutoSelectJava(boolean autoSelectJava) {
        this.autoSelectJava = autoSelectJava;
    }

    public String getDownloadSource() {
        return downloadSource;
    }

    public void setDownloadSource(String downloadSource) {
        this.downloadSource = downloadSource;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public int getSpeedLimitKb() {
        return speedLimitKb;
    }

    public void setSpeedLimitKb(int speedLimitKb) {
        this.speedLimitKb = speedLimitKb;
    }

    public double getUiOpacity() {
        return uiOpacity;
    }

    public void setUiOpacity(double uiOpacity) {
        this.uiOpacity = uiOpacity;
    }

    public boolean isAnimations() {
        return animations;
    }

    public void setAnimations(boolean animations) {
        this.animations = animations;
    }

    public double getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(double animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public String getLastInstance() {
        return lastInstance;
    }

    public void setLastInstance(String lastInstance) {
        this.lastInstance = lastInstance == null ? "" : lastInstance;
    }

    public int getMaxLogLines() {
        return maxLogLines;
    }

    public void setMaxLogLines(int maxLogLines) {
        this.maxLogLines = maxLogLines;
    }

    public boolean isIsolation() {
        return isolation;
    }

    public void setIsolation(boolean isolation) {
        this.isolation = isolation;
    }

    public boolean isAutoInstallDeps() {
        return autoInstallDeps;
    }

    public void setAutoInstallDeps(boolean autoInstallDeps) {
        this.autoInstallDeps = autoInstallDeps;
    }

    public boolean isAutoRepairLibraries() {
        return autoRepairLibraries;
    }

    public void setAutoRepairLibraries(boolean autoRepairLibraries) {
        this.autoRepairLibraries = autoRepairLibraries;
    }

    public int getAssetSampleCount() {
        return assetSampleCount;
    }

    public void setAssetSampleCount(int assetSampleCount) {
        this.assetSampleCount = assetSampleCount;
    }

    public boolean isAutoMemory() {
        return autoMemory;
    }

    public void setAutoMemory(boolean autoMemory) {
        this.autoMemory = autoMemory;
    }

    public String getUiLanguage() {
        return uiLanguage == null || uiLanguage.isBlank() ? "zh_CN" : uiLanguage;
    }

    public void setUiLanguage(String uiLanguage) {
        this.uiLanguage = uiLanguage;
    }

    /** 启动时用的副本：应用实例级内存 / JVM 覆盖，不动全局配置。 */
    public LauncherConfig copyForLaunch(Integer memoryMb, String jvmArgs) {
        return copyForLaunch(memoryMb, jvmArgs, null);
    }

    public LauncherConfig copyForLaunch(Integer memoryMb, String jvmArgs, String javaPath) {
        LauncherConfig copy = new com.google.gson.Gson()
                .fromJson(new com.google.gson.Gson().toJson(this), LauncherConfig.class);
        if (copy == null) copy = this;
        if (memoryMb != null && memoryMb > 0) copy.setMemoryMb(memoryMb);
        if (jvmArgs != null && !jvmArgs.isBlank()) copy.setJvmArgs(jvmArgs);
        if (javaPath != null && !javaPath.isBlank()) copy.setJavaPath(javaPath);
        return copy;
    }

    public String getAccentColor() {
        return accentColor == null || accentColor.isBlank() ? "共产红" : accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getBackgroundImage() {
        return backgroundImage == null ? "" : backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public double getUiFontScale() {
        return uiFontScale <= 0.5 || uiFontScale > 2.0 ? 1.0 : uiFontScale;
    }

    public void setUiFontScale(double uiFontScale) {
        this.uiFontScale = uiFontScale;
    }

    public boolean isSmartPerformance() {
        return smartPerformance;
    }

    public void setSmartPerformance(boolean smartPerformance) {
        this.smartPerformance = smartPerformance;
    }

    public boolean isPreferDiscreteGpu() {
        return preferDiscreteGpu;
    }

    public void setPreferDiscreteGpu(boolean preferDiscreteGpu) {
        this.preferDiscreteGpu = preferDiscreteGpu;
    }

    public String getGamePriority() {
        return gamePriority == null || gamePriority.isBlank() ? "normal" : gamePriority;
    }

    public void setGamePriority(String gamePriority) {
        this.gamePriority = gamePriority;
    }

    public boolean isMinimizeOnLaunch() {
        return minimizeOnLaunch;
    }

    public void setMinimizeOnLaunch(boolean minimizeOnLaunch) {
        this.minimizeOnLaunch = minimizeOnLaunch;
    }

    public boolean isPauseVisualsInGame() {
        return pauseVisualsInGame;
    }

    public void setPauseVisualsInGame(boolean pauseVisualsInGame) {
        this.pauseVisualsInGame = pauseVisualsInGame;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public String getAiApiKey() {
        return aiApiKey == null ? "" : aiApiKey;
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey;
    }

    public String getAiModel() {
        return aiModel == null || aiModel.isBlank() ? "deepseek-chat" : aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getAiBaseUrl() {
        return aiBaseUrl == null || aiBaseUrl.isBlank() ? "https://api.deepseek.com" : aiBaseUrl;
    }

    public void setAiBaseUrl(String aiBaseUrl) {
        this.aiBaseUrl = aiBaseUrl;
    }

    public boolean isPerformanceAsked() {
        return performanceAsked;
    }

    public void setPerformanceAsked(boolean performanceAsked) {
        this.performanceAsked = performanceAsked;
    }

    public boolean isPerformanceApplied() {
        return performanceApplied;
    }

    public void setPerformanceApplied(boolean performanceApplied) {
        this.performanceApplied = performanceApplied;
    }

    public String getUpdateUrl() {
        return updateUrl == null ? "" : updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public boolean isLightTheme() {
        return lightTheme;
    }

    public void setLightTheme(boolean lightTheme) {
        this.lightTheme = lightTheme;
    }

    public boolean isHardwareAcceleration() {
        return hardwareAcceleration;
    }

    public void setHardwareAcceleration(boolean hardwareAcceleration) {
        this.hardwareAcceleration = hardwareAcceleration;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }
}
