package org.example.CampusMart.web.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class MediaUrlBuilder {

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    public String toPublicUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return objectName;
        }
        if (objectName.startsWith("http://") || objectName.startsWith("https://")) {
            return objectName;
        }

        String path = "/app/files/" + encodeObjectName(objectName);
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return path;
        }
        return publicBaseUrl.replaceAll("/+$", "") + path;
    }

    private String encodeObjectName(String objectName) {
        return Arrays.stream(objectName.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }
}
