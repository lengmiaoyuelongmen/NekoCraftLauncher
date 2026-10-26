package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 多账户列表，不保存密码。 */
public final class AccountStore {

    public record Account(String username, String uuid, String accessToken, String loginType,
                          String userType, String server, String skinUrl) {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AccountStore() {
    }

    private static File file() {
        return new File(AppPaths.dataDir(), "accounts.json");
    }

    public static List<Account> list() {
        File f = file();
        if (!f.isFile()) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonArray()) return new ArrayList<>();
            List<Account> result = new ArrayList<>();
            for (JsonElement element : parsed.getAsJsonArray()) {
                JsonObject o = element.getAsJsonObject();
                result.add(new Account(get(o, "username"), get(o, "uuid"), get(o, "accessToken"),
                        get(o, "loginType"), get(o, "userType"), get(o, "server"), get(o, "skinUrl")));
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void upsert(UserSession session, String loginType, String server) {
        if (session == null || session.getUsername() == null || session.getUsername().isBlank()) return;
        List<Account> accounts = list();
        accounts.removeIf(a -> a.username().equalsIgnoreCase(session.getUsername()));
        accounts.add(0, new Account(session.getUsername(),
                session.getUuid() == null ? "" : session.getUuid().toString(),
                session.getAccessToken() == null ? "" : session.getAccessToken(),
                loginType == null ? "" : loginType,
                session.getUserType() == null ? "" : session.getUserType(),
                server == null ? "" : server,
                session.getSkinUrl() == null ? "" : session.getSkinUrl()));
        save(accounts);
    }

    public static void remove(String username) {
        List<Account> accounts = list();
        accounts.removeIf(a -> a.username().equalsIgnoreCase(username));
        save(accounts);
    }

    private static void save(List<Account> accounts) {
        JsonArray array = new JsonArray();
        for (Account account : accounts) {
            JsonObject o = new JsonObject();
            o.addProperty("username", account.username());
            o.addProperty("uuid", account.uuid());
            o.addProperty("accessToken", account.accessToken());
            o.addProperty("loginType", account.loginType());
            o.addProperty("userType", account.userType());
            o.addProperty("server", account.server());
            o.addProperty("skinUrl", account.skinUrl());
            array.add(o);
        }
        try (Writer writer = Files.newBufferedWriter(file().toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(array, writer);
        } catch (Exception ignored) {
        }
    }

    private static String get(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    public static UserSession toSession(Account account) {
        UserSession session = new UserSession();
        session.setUsername(account.username());
        session.setAccessToken(account.accessToken());
        session.setUserType(account.userType());
        session.setAuthServerUrl(account.server());
        session.setSkinUrl(account.skinUrl());
        session.setType(account.loginType().contains("微软") ? UserSession.LoginType.MICROSOFT
                : account.loginType().contains("第三方") ? UserSession.LoginType.THIRD_PARTY
                : UserSession.LoginType.OFFLINE);
        try {
            if (account.uuid() != null && !account.uuid().isBlank()) session.setUuid(UUID.fromString(account.uuid()));
        } catch (Exception ignored) {
        }
        return session;
    }
}
