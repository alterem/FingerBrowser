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
            objectMapper.writeValue(new File(filePath), profile);
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

        try (var stream = Files.list(Paths.get(profilesDirectory))) {
            stream.filter(path -> path.toString().endsWith(".json"))
                  .forEach(path -> {
                      try {
                          BrowserProfile profile = objectMapper.readValue(path.toFile(), BrowserProfile.class);
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
                try (var stream = Files.list(file.toPath())) {
                    stream.filter(path -> path.toString().endsWith(".json"))
                          .forEach(path -> {
                              try {
                                  BrowserProfile profile = objectMapper.readValue(path.toFile(), BrowserProfile.class);
                                  profiles.add(profile);
                                  importedProfiles.add(profile);
                                  saveProfile(profile);
                              } catch (IOException e) {
                                  log.error("Failed to import profile from file: {}", path, e);
                              }
                          });
                }
            } else {
                // 导入单个文件
                BrowserProfile profile = objectMapper.readValue(file, BrowserProfile.class);
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
            objectMapper.writeValue(destination, profile);
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
            String safeName = profile.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
            File destination = new File(directory, safeName + "_" + profile.getId() + ".json");
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
     * 获取所有配置文件的列表
     */
    public List<BrowserProfile> getAllProfiles() {
        return new ArrayList<>(profiles);
    }
}
