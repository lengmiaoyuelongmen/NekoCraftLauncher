package com.example.demo;

import java.util.prefs.Preferences;

/** 敏感项存储：Windows 下由 java.util.prefs 落到注册表 HKCU，不进入 config.json。 */
public final class SecretStore {

    private static final Preferences PREFS =
            Preferences.userRoot().node("NekoCraftLauncher");

    private SecretStore() {
    }

    public static String loadAiApiKey() {
        try {
            return PREFS.get("aiApiKey", "");
        } catch (Exception e) {
            return "";
        }
    }

    public static void saveAiApiKey(String key) {
        try {
            if (key == null || key.isBlank()) PREFS.remove("aiApiKey");
            else PREFS.put("aiApiKey", key);
            PREFS.flush();
        } catch (Exception ignored) {
        }
    }

    public static String location() {
        // java.util.prefs 在 Windows 上会把大写字母编码成 "/X"，所以
        // 节点 NekoCraftLauncher 实际落在 Prefs\ 下的 Neko/Craft/Launcher
        return "HKCU\\Software\\JavaSoft\\Prefs\\Neko/Craft/Launcher（值名 ai/Api/Key）";
    }
}
