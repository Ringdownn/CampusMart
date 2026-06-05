package org.example.CampusMart.web.controller.goods;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.example.CampusMart.common.minio.MinioProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
public class FileController {

    private static final String FILE_PREFIX = "/app/files/";

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @GetMapping("/app/files/**")
    public ResponseEntity<StreamingResponseBody> getFile(HttpServletRequest request) {
        try {
            String objectName = getObjectName(request);
            if (objectName.isBlank() || objectName.contains("..")) {
                return ResponseEntity.badRequest().build();
            }

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );

            StreamingResponseBody body = outputStream -> {
                try (var inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(objectName)
                                .build()
                )) {
                    inputStream.transferTo(outputStream);
                } catch (Exception e) {
                    throw new IOException("Failed to stream object from MinIO: " + objectName, e);
                }
            };

            MediaType mediaType = stat.contentType() == null || stat.contentType().isBlank() || stat.contentType().contains("*")
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(stat.contentType());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private String getObjectName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(FILE_PREFIX);
        if (index < 0) {
            return "";
        }
        String encoded = uri.substring(index + FILE_PREFIX.length());
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }
}
