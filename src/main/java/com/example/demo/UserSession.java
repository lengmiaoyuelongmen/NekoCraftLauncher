package com.example.demo;

import java.util.UUID;

public class UserSession {
    public enum LoginType { OFFLINE, MICROSOFT, THIRD_PARTY }

    private LoginType type;
    private String username;
    private UUID uuid;
    private String accessToken;
    private String userType;
    private String authServerUrl;
    private String skinUrl;

    public LoginType getType() { return type; }
    public void setType(LoginType type) { this.type = type; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getAuthServerUrl() { return authServerUrl; }
    public void setAuthServerUrl(String authServerUrl) { this.authServerUrl = authServerUrl; }
    public String getSkinUrl() { return skinUrl; }
    public void setSkinUrl(String skinUrl) { this.skinUrl = skinUrl; }
}