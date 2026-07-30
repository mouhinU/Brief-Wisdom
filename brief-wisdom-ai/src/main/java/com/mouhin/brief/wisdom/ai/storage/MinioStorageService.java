package com.mouhin.brief.wisdom.ai.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO/S3 兼容对象存储实现
 * <p>
 * 基于 MinIO Java SDK（S3 兼容协议），同样适用于阿里云 OSS、AWS S3 等兼容端点。
 * 通过 {@code app.storage.minio.*} 配置端点、凭证与存储桶，{@code resolveUrl} 返回预签名 URL。
 * 适用于生产与多实例部署。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    /**
     * 日期分目录格式（小写 y，遵循项目日期规范）
     */
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties storageProperties;
    private final MinioClient minioClient;

    public MinioStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        StorageProperties.Minio minio = storageProperties.getMinio();
        this.minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Override
    public StoredObject upload(InputStream input, String fileName, String contentType, long size) {
        String bucket = storageProperties.getMinio().getBucket();
        String key = buildKey(fileName);
        ensureBucketExists(bucket);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(input, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("[存储] MinIO 文件上传失败 - bucket: {}, key: {}, error: {}", bucket, key, e.getMessage(), e);
            throw new StorageException("文件上传失败", e);
        }
        String url = resolveUrl(key);
        log.info("[存储] MinIO 文件上传成功 - bucket: {}, key: {}, size: {}", bucket, key, size);
        return new StoredObject(key, url, size, contentType);
    }

    @Override
    public byte[] readBytes(String key) {
        String bucket = storageProperties.getMinio().getBucket();
        try (InputStream input = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build())) {
            return input.readAllBytes();
        } catch (Exception e) {
            log.error("[存储] MinIO 文件读取失败 - bucket: {}, key: {}, error: {}", bucket, key, e.getMessage(), e);
            throw new StorageException("文件读取失败", e);
        }
    }

    @Override
    public String resolveUrl(String key) {
        String bucket = storageProperties.getMinio().getBucket();
        int expireSeconds = storageProperties.getMinio().getUrlExpireSeconds();
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("[存储] MinIO 预签名 URL 生成失败 - bucket: {}, key: {}, error: {}", bucket, key, e.getMessage(), e);
            throw new StorageException("文件地址解析失败", e);
        }
    }

    @Override
    public void delete(String key) {
        String bucket = storageProperties.getMinio().getBucket();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
            log.info("[存储] MinIO 文件删除成功 - bucket: {}, key: {}", bucket, key);
        } catch (Exception e) {
            log.error("[存储] MinIO 文件删除失败 - bucket: {}, key: {}, error: {}", bucket, key, e.getMessage(), e);
            throw new StorageException("文件删除失败", e);
        }
    }

    /**
     * 确保存储桶存在，不存在则创建
     *
     * @param bucket 存储桶名称
     */
    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("[存储] 创建 MinIO 存储桶 - bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("[存储] MinIO 存储桶检查/创建失败 - bucket: {}, error: {}", bucket, e.getMessage(), e);
            throw new StorageException("存储桶初始化失败", e);
        }
    }

    /**
     * 生成存储 key：日期分目录 + UUID + 原始扩展名
     *
     * @param fileName 原始文件名
     * @return 存储 key
     */
    private String buildKey(String fileName) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String extension = extractExtension(fileName);
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 提取文件扩展名（含点号），无扩展名时返回空字符串
     *
     * @param fileName 原始文件名
     * @return 扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase();
    }
}
