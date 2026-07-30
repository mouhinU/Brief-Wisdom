package com.mouhin.brief.wisdom.ai.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储实现
 * <p>
 * 文件按 {@code yyyy/MM/dd/uuid.ext} 结构落盘到 {@code app.storage.local.base-dir}，
 * 通过 {@code app.storage.local.url-prefix} 前缀经静态资源映射对外提供访问。
 * 适用于单机与开发环境；多实例部署请切换为 MinIO/S3 兼容存储。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    /**
     * 日期分目录格式（小写 y，遵循项目日期规范）
     */
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties storageProperties;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredObject upload(InputStream input, String fileName, String contentType, long size) {
        String key = buildKey(fileName);
        Path target = resolveWithinBaseDir(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("[存储] 本地文件写入失败 - key: {}, error: {}", key, e.getMessage(), e);
            throw new StorageException("文件上传失败", e);
        }
        String url = resolveUrl(key);
        log.info("[存储] 本地文件上传成功 - key: {}, size: {}, contentType: {}", key, size, contentType);
        return new StoredObject(key, url, size, contentType);
    }

    @Override
    public byte[] readBytes(String key) {
        Path path = resolveWithinBaseDir(key);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("[存储] 本地文件读取失败 - key: {}, error: {}", key, e.getMessage(), e);
            throw new StorageException("文件读取失败", e);
        }
    }

    @Override
    public String resolveUrl(String key) {
        String urlPrefix = storageProperties.getLocal().getUrlPrefix();
        if (urlPrefix.endsWith("/")) {
            urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
        }
        return urlPrefix + "/" + key;
    }

    @Override
    public void delete(String key) {
        Path path = resolveWithinBaseDir(key);
        try {
            boolean deleted = Files.deleteIfExists(path);
            log.info("[存储] 本地文件删除 - key: {}, deleted: {}", key, deleted);
        } catch (IOException e) {
            log.error("[存储] 本地文件删除失败 - key: {}, error: {}", key, e.getMessage(), e);
            throw new StorageException("文件删除失败", e);
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

    /**
     * 将存储 key 解析为 base-dir 内的绝对路径，并校验防止路径穿越
     *
     * @param key 存储 key
     * @return base-dir 内的绝对路径
     */
    private Path resolveWithinBaseDir(String key) {
        Path baseDir = Paths.get(storageProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            log.warn("[存储] 检测到非法存储 key（路径穿越） - key: {}", key);
            throw new StorageException("非法的存储路径");
        }
        return resolved;
    }
}
