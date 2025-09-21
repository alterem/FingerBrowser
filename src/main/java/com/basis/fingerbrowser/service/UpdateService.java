package com.basis.fingerbrowser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * 轻量级更新检查服务
 * 说明：在无网络环境下应短路；生产中可配合签名校验与发布渠道。
 */
public class UpdateService {
    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);

    private static final String LATEST_URL = "https://raw.githubusercontent.com/alterem/fingerbrowser/main/latest.json";
    private static final String RELEASES_URL = "https://github.com/alterem/fingerbrowser/releases";

    public static Optional<String> fetchLatestVersion() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LATEST_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String body = resp.body();
                String ver = parseVersionFromJson(body);
                return Optional.ofNullable(ver);
            }
        } catch (Exception e) {
            log.info("Update check skipped/failed: {}", e.toString());
        }
        return Optional.empty();
    }

    private static String parseVersionFromJson(String json) {
        // 极简解析：查找 "version":"x.y.z"
        int idx = json.indexOf("\"version\"");
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        int q1 = json.indexOf('"', colon + 1);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        return json.substring(q1 + 1, q2).trim();
    }

    public static Optional<String> parseNotesFromJson(String json) {
        int idx = json.indexOf("\"notes\"");
        if (idx < 0) return Optional.empty();
        int colon = json.indexOf(":", idx);
        int q1 = json.indexOf('"', colon + 1);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return Optional.empty();
        return Optional.of(json.substring(q1 + 1, q2).trim());
    }

    public static boolean isNewer(String latest, String current) {
        if (latest == null || current == null) return false;
        String[] la = latest.split("\\.");
        String[] ca = current.split("\\.");
        int n = Math.max(la.length, ca.length);
        for (int i = 0; i < n; i++) {
            int li = i < la.length ? safeParseInt(la[i]) : 0;
            int ci = i < ca.length ? safeParseInt(ca[i]) : 0;
            if (li != ci) return li > ci;
        }
        return false;
    }

    private static int safeParseInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }

    public static String getReleasesUrl() {
        return RELEASES_URL;
    }
}
