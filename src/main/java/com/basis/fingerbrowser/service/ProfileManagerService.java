package com.basis.fingerbrowser.service;

import com.basis.fingerbrowser.model.BrowserProfile;
import com.basis.fingerbrowser.model.ProxySettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ProfileManagerService {

    private static final Logger log = LoggerFactory.getLogger(ProfileManagerService.class);
    
    private final String profilesDirectory;
    private final ObservableList<BrowserProfile> profiles;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final ObjectMapper objectMapper;

    public ProfileManagerService(String profilesDirectory) {
        this.profilesDirectory = profilesDirectory;
        this.profiles = FXCollections.observableArrayList();

        // 初始化Jackson ObjectMapper
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new JavaTimeModule());

        // 确保配置目录存在
        createDirectoryIfNotExists(profilesDirectory);

        // 加载现有配置文件
        loadProfiles();
    }

    /**
     * 获取所有配置文件
     */
    public ObservableList<BrowserProfile> getProfiles() {
        return profiles;
    }

    /**
     * 添加新配置文件
     */
    public void addProfile(BrowserProfile profile) {
        profiles.add(profile);
        saveProfile(profile);
    }

    /**
     * 更新配置文件
     */
    public void updateProfile(BrowserProfile profile) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(profile.getId())) {
                profiles.set(i, profile);
                break;
            }
        }
        saveProfile(profile);
    }

    /**
     * 删除配置文件
     */
    public void deleteProfile(String profileId) {
        // 从内存中移除
        profiles.removeIf(p -> p.getId().equals(profileId));

        // 从磁盘中删除
        try {
            Path profilePath = Paths.get(profilesDirectory, profileId + ".json");
            Files.deleteIfExists(profilePath);
        } catch (IOException e) {
            log.error("Failed to delete profile file for ID: {}", profileId, e);
        }
    }

    /**
     * 获取指定配置文件
     */
    public BrowserProfile getProfile(String profileId) {
        Optional<BrowserProfile> profile = profiles.stream()
                .filter(p -> p.getId().equals(profileId))
                .findFirst();
        return profile.orElse(null);
    }

    /**
     * 保存单个配置文件
     */
    private void saveProfile(BrowserProfile profile) {
        try {
            String filePath = profilesDirectory + File.separator + profile.getId() + ".json";

            // 使用Jackson将对象转换为JSON并写入文件
            objectMapper.writeValue(new File(filePath), profileToMap(profile));

        } catch (IOException e) {
            log.error("Failed to save profile '{}' (ID: {})", profile.getName(), profile.getId(), e);
        }
    }

    /**
     * 保存所有配置文件
     */
    public void saveAllProfiles() {
        for (BrowserProfile profile : profiles) {
            saveProfile(profile);
        }
    }

    /**
     * 加载所有配置文件
     */
    private void loadProfiles() {
        profiles.clear();

        try {
            Files.list(Paths.get(profilesDirectory))
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            // 使用Jackson从文件读取JSON
                            Map<String, Object> profileMap = objectMapper.readValue(path.toFile(), Map.class);
                            BrowserProfile profile = mapToProfile(profileMap);
                            profiles.add(profile);
                        } catch (IOException e) {
                            log.error("Failed to load profile from file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to load profiles from directory: {}", profilesDirectory, e);
        }
    }

    /**
     * 导入配置文件
     */
    public List<BrowserProfile> importProfiles(File file) {
        List<BrowserProfile> importedProfiles = new ArrayList<>();

        try {
            if (file.isDirectory()) {
                // 导入目录中的所有.json文件
                Files.list(file.toPath())
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                // 使用Jackson从文件读取JSON
                                Map<String, Object> profileMap = objectMapper.readValue(path.toFile(), Map.class);
                                BrowserProfile profile = mapToProfile(profileMap);
                                profiles.add(profile);
                                importedProfiles.add(profile);
                                saveProfile(profile);
                            } catch (IOException e) {
                                log.error("Failed to import profile from file: {}", path, e);
                            }
                        });
            } else {
                // 导入单个文件
                Map<String, Object> profileMap = objectMapper.readValue(file, Map.class);
                BrowserProfile profile = mapToProfile(profileMap);
                profiles.add(profile);
                importedProfiles.add(profile);
                saveProfile(profile);
            }
        } catch (IOException e) {
            log.error("Failed to import profile(s) from: {}", file.getPath(), e);
        }

        return importedProfiles;
    }

    /**
     * 导出配置文件
     */
    public boolean exportProfile(BrowserProfile profile, File destination) {
        try {
            // 使用Jackson将对象转换为JSON并写入文件
            objectMapper.writeValue(destination, profileToMap(profile));
            return true;
        } catch (IOException e) {
            log.error("Failed to export profile '{}' to: {}", profile.getName(), destination.getPath(), e);
            return false;
        }
    }

    /**
     * 导出所有配置文件
     */
    public boolean exportAllProfiles(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }

        boolean success = true;
        for (BrowserProfile profile : profiles) {
            File destination = new File(directory, profile.getName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".json");
            success &= exportProfile(profile, destination);
        }

        return success;
    }

    private void createDirectoryIfNotExists(String dirPath) {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("Failed to create profiles directory: {}", dirPath, e);
            }
        }
    }

    /**
     * 将配置对象转换为Map（用于Jackson序列化）
     */
    private Map<String, Object> profileToMap(BrowserProfile profile) {
        Map<String, Object> map = new HashMap<>();

        // 基本信息
        map.put("id", profile.getId());
        map.put("name", profile.getName());
        map.put("createdAt", profile.getCreatedAt().format(DATE_TIME_FORMATTER));
        map.put("active", profile.isActive());

        if (profile.getLastUsed() != null) {
            map.put("lastUsed", profile.getLastUsed().format(DATE_TIME_FORMATTER));
        }

        // 基本浏览器设置
        if (profile.getUserAgent() != null) map.put("userAgent", profile.getUserAgent());
        if (profile.getPlatform() != null) map.put("platform", profile.getPlatform());
        if (profile.getLanguage() != null) map.put("language", profile.getLanguage());
        if (profile.getTimezone() != null) map.put("timezone", profile.getTimezone());
        if (profile.getResolution() != null) map.put("resolution", profile.getResolution());
        if (profile.getBrowserExecutablePath() != null) map.put("browserExecutablePath", profile.getBrowserExecutablePath());
        if (profile.getUserDataDir() != null) map.put("userDataDir", profile.getUserDataDir());
        if (profile.getNotes() != null) map.put("notes", profile.getNotes());

        // WebRTC设置
        if (profile.getWebRTCSettings() != null) {
            map.put("webRTCSettings", profile.getWebRTCSettings());
        }

        // Canvas指纹设置
        if (profile.getCanvasFingerprint() != null) {
            map.put("canvasFingerprint", profile.getCanvasFingerprint());
        }

        // 字体指纹设置
        if (profile.getFontFingerprint() != null) {
            map.put("fontFingerprint", profile.getFontFingerprint());
        }

        // 代理设置
        if (profile.getProxySettings() != null) {
            ProxySettings proxy = profile.getProxySettings();
            Map<String, Object> proxyMap = new HashMap<>();
            proxyMap.put("enabled", proxy.isEnabled());

            if (proxy.getType() != null) proxyMap.put("type", proxy.getType());
            if (proxy.getHost() != null) proxyMap.put("host", proxy.getHost());
            if (proxy.getPort() > 0) proxyMap.put("port", proxy.getPort());
            if (proxy.getUsername() != null) proxyMap.put("username", proxy.getUsername());
            if (proxy.getPassword() != null) proxyMap.put("password", proxy.getPassword());

            map.put("proxySettings", proxyMap);
        }

        return map;
    }

    /**
     * 从Map解析配置对象（用于Jackson反序列化）
     */
    private BrowserProfile mapToProfile(Map<String, Object> map) {
        String id = (String) map.getOrDefault("id", UUID.randomUUID().toString());
        String name = (String) map.getOrDefault("name", "未命名配置");

        LocalDateTime createdAt = LocalDateTime.now();
        if (map.containsKey("createdAt") && map.get("createdAt") != null) {
            createdAt = LocalDateTime.parse((String) map.get("createdAt"), DATE_TIME_FORMATTER);
        }

        BrowserProfile profile = new BrowserProfile(id, name, createdAt);

        // 基本浏览器设置
        if (map.containsKey("userAgent")) profile.setUserAgent((String) map.get("userAgent"));
        if (map.containsKey("platform")) profile.setPlatform((String) map.get("platform"));
        if (map.containsKey("language")) profile.setLanguage((String) map.get("language"));
        if (map.containsKey("timezone")) profile.setTimezone((String) map.get("timezone"));
        if (map.containsKey("resolution")) profile.setResolution((String) map.get("resolution"));
        if (map.containsKey("browserExecutablePath")) profile.setBrowserExecutablePath((String) map.get("browserExecutablePath"));
        if (map.containsKey("userDataDir")) profile.setUserDataDir((String) map.get("userDataDir"));
        if (map.containsKey("notes")) profile.setNotes((String) map.get("notes"));

        // 活动状态和上次使用时间
        if (map.containsKey("active")) profile.setActive((Boolean) map.get("active"));
        if (map.containsKey("lastUsed") && map.get("lastUsed") != null && !((String) map.get("lastUsed")).isEmpty()) {
            profile.setLastUsed(LocalDateTime.parse((String) map.get("lastUsed"), DATE_TIME_FORMATTER));
        }

        // WebRTC设置
        if (map.containsKey("webRTCSettings") && map.get("webRTCSettings") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> webrtcSettings = (Map<String, Object>) map.get("webRTCSettings");
            profile.setWebRTCSettings(webrtcSettings);
        }

        // Canvas指纹设置
        if (map.containsKey("canvasFingerprint") && map.get("canvasFingerprint") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> canvasSettings = (Map<String, Object>) map.get("canvasFingerprint");
            profile.setCanvasFingerprint(canvasSettings);
        }

        // 字体指纹设置
        if (map.containsKey("fontFingerprint") && map.get("fontFingerprint") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fontSettings = (Map<String, Object>) map.get("fontFingerprint");
            profile.setFontFingerprint(fontSettings);
        }

        // 代理设置
        if (map.containsKey("proxySettings") && map.get("proxySettings") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> proxyMap = (Map<String, Object>) map.get("proxySettings");
            ProxySettings proxySettings = new ProxySettings();

            if (proxyMap.containsKey("enabled")) proxySettings.setEnabled((Boolean) proxyMap.get("enabled"));
            if (proxyMap.containsKey("type")) proxySettings.setType((String) proxyMap.get("type"));
            if (proxyMap.containsKey("host")) proxySettings.setHost((String) proxyMap.get("host"));
            if (proxyMap.containsKey("port")) {
                Object portObj = proxyMap.get("port");
                if (portObj instanceof Integer) {
                    proxySettings.setPort((Integer) portObj);
                } else if (portObj instanceof Double) {
                    // Jackson可能将数字解析为Double
                    proxySettings.setPort(((Double) portObj).intValue());
                }
            }
            if (proxyMap.containsKey("username")) proxySettings.setUsername((String) proxyMap.get("username"));
            if (proxyMap.containsKey("password")) proxySettings.setPassword((String) proxyMap.get("password"));

            profile.setProxySettings(proxySettings);
        }

        return profile;
    }

    /**
     * 获取所有配置文件的列表
     */
    public List<BrowserProfile> getAllProfiles() {
        return new ArrayList<>(profiles);
    }
}
